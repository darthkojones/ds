package com.webcrawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 * Extracts links from HTML content using JSoup.
 */
public class LinkExtractor {
    private static final Logger logger = LoggerFactory.getLogger(LinkExtractor.class);

    /**
     * Extracts all valid HTTP/HTTPS links from HTML content.
     * 
     * @param html The HTML content
     * @param baseUrl The base URL for resolving relative links
     * @return Set of absolute URLs
     */
    public Set<String> extractLinks(String html, String baseUrl) {
        Set<String> links = new HashSet<>();

        try {
            Document doc = Jsoup.parse(html, baseUrl);
            Elements linkElements = doc.select("a[href]");

            for (Element link : linkElements) {
                try {
                    String href = link.attr("abs:href"); // Get absolute URL
                    
                    if (isValidUrl(href)) {
                        links.add(href);
                    }
                } catch (Exception e) {
                    logger.debug("Failed to process link: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("Failed to parse HTML from {}: {}", baseUrl, e.getMessage());
        }

        return links;
    }

    /**
     * Validates if a URL is valid for crawling.
     * 
     * @param url The URL to validate
     * @return true if valid HTTP/HTTPS URL
     */
    private boolean isValidUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }

        // Only accept HTTP and HTTPS URLs
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return false;
        }

        // Skip common file extensions that are not HTML pages
        String lowerUrl = url.toLowerCase();
        if (lowerUrl.matches(".*\\.(jpg|jpeg|png|gif|pdf|zip|exe|dmg|mp4|mp3|css|js|ico|svg)$")) {
            return false;
        }

        // Validate URL format
        try {
            new URL(url);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extracts the page title from HTML content.
     * 
     * @param html The HTML content
     * @return The page title or empty string
     */
    public String extractTitle(String html) {
        try {
            Document doc = Jsoup.parse(html);
            return doc.title();
        } catch (Exception e) {
            logger.debug("Failed to extract title: {}", e.getMessage());
            return "";
        }
    }
}
