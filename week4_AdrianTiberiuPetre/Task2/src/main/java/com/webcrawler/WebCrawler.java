package com.webcrawler;

import com.webcrawler.UrlManager.UrlDepthPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Main orchestrator for the web crawler with thread pool management.
 */
public class WebCrawler {
    private static final Logger logger = LoggerFactory.getLogger(WebCrawler.class);

    private final String rootUrl;
    private final int maxDepth;
    private final int maxPages;
    private final int threadPoolSize;
    private final boolean stayInDomain;
    private final String outputPath;

    private UrlManager urlManager;
    private ContentSaver contentSaver;
    private LinkExtractor linkExtractor;
    private ExecutorService executorService;
    private AtomicInteger activeThreads;

    /**
     * Builder for WebCrawler configuration.
     */
    public static class Builder {
        private String rootUrl;
        private int maxDepth = 3;
        private int maxPages = 100;
        private int threadPoolSize = 10;
        private boolean stayInDomain = true;
        private String outputPath = "crawled_data";

        public Builder rootUrl(String rootUrl) {
            this.rootUrl = rootUrl;
            return this;
        }

        public Builder maxDepth(int maxDepth) {
            this.maxDepth = maxDepth;
            return this;
        }

        public Builder maxPages(int maxPages) {
            this.maxPages = maxPages;
            return this;
        }

        public Builder threadPoolSize(int threadPoolSize) {
            this.threadPoolSize = threadPoolSize;
            return this;
        }

        public Builder stayInDomain(boolean stayInDomain) {
            this.stayInDomain = stayInDomain;
            return this;
        }

        public Builder outputPath(String outputPath) {
            this.outputPath = outputPath;
            return this;
        }

        public WebCrawler build() {
            if (rootUrl == null || rootUrl.isEmpty()) {
                throw new IllegalArgumentException("Root URL must be specified");
            }
            return new WebCrawler(this);
        }
    }

    /**
     * Private constructor - use Builder to create instances.
     */
    private WebCrawler(Builder builder) {
        this.rootUrl = builder.rootUrl;
        this.maxDepth = builder.maxDepth;
        this.maxPages = builder.maxPages;
        this.threadPoolSize = builder.threadPoolSize;
        this.stayInDomain = builder.stayInDomain;
        this.outputPath = builder.outputPath;
        this.activeThreads = new AtomicInteger(0);
    }

    /**
     * Starts the crawling process.
     */
    public void start() {
        logger.info("=================================================");
        logger.info("Starting Web Crawler");
        logger.info("=================================================");
        logger.info("Root URL: {}", rootUrl);
        logger.info("Max Depth: {}", maxDepth);
        logger.info("Max Pages: {}", maxPages);
        logger.info("Thread Pool Size: {}", threadPoolSize);
        logger.info("Stay in Domain: {}", stayInDomain);
        logger.info("Output Path: {}", outputPath);
        logger.info("=================================================");

        // Initialize components
        urlManager = new UrlManager(rootUrl, stayInDomain);
        contentSaver = new ContentSaver(outputPath);
        linkExtractor = new LinkExtractor();
        executorService = Executors.newFixedThreadPool(threadPoolSize);

        // Add the root URL to start crawling
        urlManager.addUrl(rootUrl, 0);

        // Main crawling loop
        long startTime = System.currentTimeMillis();
        
        while (shouldContinueCrawling()) {
            UrlDepthPair urlDepthPair = urlManager.getNextUrl();
            
            if (urlDepthPair != null) {
                activeThreads.incrementAndGet();
                
                CrawlerTask task = new CrawlerTask(
                    urlDepthPair.getUrl(),
                    urlDepthPair.getDepth(),
                    maxDepth,
                    urlManager,
                    contentSaver,
                    linkExtractor
                );

                executorService.submit(() -> {
                    try {
                        task.run();
                    } finally {
                        activeThreads.decrementAndGet();
                    }
                });
            } else {
                // No URLs in queue, wait a bit for active threads to finish
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    logger.warn("Interrupted while waiting: {}", e.getMessage());
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        // Shutdown and wait for completion
        shutdown();

        long endTime = System.currentTimeMillis();
        long duration = (endTime - startTime) / 1000;

        // Create summary
        contentSaver.createSummary(
            urlManager.getCrawledCount(),
            urlManager.getTotalDiscoveredUrls(),
            rootUrl
        );

        // Print final statistics
        printStatistics(duration);
    }

    /**
     * Determines if crawling should continue.
     * 
     * @return true if should continue crawling
     */
    private boolean shouldContinueCrawling() {
        // Stop if max pages reached
        if (urlManager.getCrawledCount() >= maxPages) {
            logger.info("Max pages limit ({}) reached", maxPages);
            return false;
        }

        // Stop if no more URLs and no active threads
        if (!urlManager.hasMoreUrls() && activeThreads.get() == 0) {
            logger.info("No more URLs to crawl and no active threads");
            return false;
        }

        return true;
    }

    /**
     * Shuts down the executor service and waits for completion.
     */
    private void shutdown() {
        logger.info("Shutting down crawler...");
        
        executorService.shutdown();
        
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                logger.warn("Executor did not terminate in time, forcing shutdown");
                executorService.shutdownNow();
                
                if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                    logger.error("Executor did not terminate");
                }
            }
        } catch (InterruptedException e) {
            logger.error("Shutdown interrupted: {}", e.getMessage());
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Prints crawling statistics.
     * 
     * @param durationSeconds Duration in seconds
     */
    private void printStatistics(long durationSeconds) {
        logger.info("=================================================");
        logger.info("Crawling Completed!");
        logger.info("=================================================");
        logger.info("Total Pages Crawled: {}", urlManager.getCrawledCount());
        logger.info("Total URLs Discovered: {}", urlManager.getTotalDiscoveredUrls());
        logger.info("Duration: {} seconds", durationSeconds);
        logger.info("Output Directory: {}", contentSaver.getOutputDirectory().getAbsolutePath());
        logger.info("=================================================");
    }
}
