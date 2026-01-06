package com.webcrawler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main application entry point for the Web Crawler.
 * 
 * This multi-threaded web crawler starts from a root URL and crawls pages,
 * extracting links and saving content locally. Each URL is processed in a
 * separate thread from a thread pool.
 */
public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        try {
            // Configure and build the web crawler
            WebCrawler crawler = new WebCrawler.Builder()
                .rootUrl("https://example.com/")
                .maxDepth(3)               // Crawl up to 3 levels deep
                .maxPages(50)              // Stop after 50 pages
                .threadPoolSize(10)        // Use 10 concurrent threads
                .stayInDomain(true)        // Only crawl example.com domain
                .outputPath("crawled_data") // Save to this directory
                .build();

            // Start crawling
            crawler.start();

        } catch (Exception e) {
            logger.error("Fatal error in web crawler: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
}
