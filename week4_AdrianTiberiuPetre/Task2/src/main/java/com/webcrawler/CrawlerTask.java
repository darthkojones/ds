package com.webcrawler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Runnable task that crawls a single URL and extracts links.
 */
public class CrawlerTask implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(CrawlerTask.class);
    private static final int TIMEOUT_MS = 5000; // 5 seconds
    private static final String USER_AGENT = "Mozilla/5.0 (WebCrawler/1.0)";

    private final String url;
    private final int depth;
    private final int maxDepth;
    private final UrlManager urlManager;
    private final ContentSaver contentSaver;
    private final LinkExtractor linkExtractor;

    /**
     * Constructs a CrawlerTask.
     * 
     * @param url The URL to crawl
     * @param depth The current depth level
     * @param maxDepth The maximum depth to crawl
     * @param urlManager The URL manager
     * @param contentSaver The content saver
     * @param linkExtractor The link extractor
     */
    public CrawlerTask(String url, int depth, int maxDepth, 
                       UrlManager urlManager, ContentSaver contentSaver, 
                       LinkExtractor linkExtractor) {
        this.url = url;
        this.depth = depth;
        this.maxDepth = maxDepth;
        this.urlManager = urlManager;
        this.contentSaver = contentSaver;
        this.linkExtractor = linkExtractor;
    }

    @Override
    public void run() {
        try {
            logger.info("Crawling (depth {}): {}", depth, url);

            // Fetch the page content
            String content = fetchContent(url);
            
            if (content == null || content.isEmpty()) {
                logger.warn("No content retrieved from: {}", url);
                return;
            }

            // Extract title
            String title = linkExtractor.extractTitle(content);

            // Save content to file
            contentSaver.saveContent(url, content, depth, title);

            // Increment crawled count
            int crawledCount = urlManager.incrementCrawledCount();
            logger.info("Progress: {} pages crawled, {} URLs discovered", 
                       crawledCount, urlManager.getTotalDiscoveredUrls());

            // Extract links if we haven't reached max depth
            if (depth < maxDepth) {
                Set<String> links = linkExtractor.extractLinks(content, url);
                
                logger.debug("Found {} links on: {}", links.size(), url);

                // Add new links to the queue
                int addedCount = 0;
                for (String link : links) {
                    if (urlManager.addUrl(link, depth + 1)) {
                        addedCount++;
                    }
                }

                if (addedCount > 0) {
                    logger.debug("Added {} new URLs to queue from: {}", addedCount, url);
                }
            } else {
                logger.debug("Max depth reached, not extracting links from: {}", url);
            }

        } catch (Exception e) {
            logger.error("Error crawling {}: {}", url, e.getMessage(), e);
        }
    }

    /**
     * Fetches the HTML content from a URL.
     * 
     * @param urlString The URL to fetch
     * @return The HTML content or null if failed
     */
    private String fetchContent(String urlString) {
        HttpURLConnection connection = null;
        
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            
            // Set connection properties
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setInstanceFollowRedirects(true);

            // Check response code
            int responseCode = connection.getResponseCode();
            
            if (responseCode != HttpURLConnection.HTTP_OK) {
                logger.warn("HTTP {} for URL: {}", responseCode, urlString);
                return null;
            }

            // Check content type
            String contentType = connection.getContentType();
            if (contentType != null && !contentType.contains("text/html")) {
                logger.debug("Skipping non-HTML content: {} ({})", urlString, contentType);
                return null;
            }

            // Read content
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }

            return content.toString();

        } catch (Exception e) {
            logger.error("Failed to fetch content from {}: {}", urlString, e.getMessage());
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
