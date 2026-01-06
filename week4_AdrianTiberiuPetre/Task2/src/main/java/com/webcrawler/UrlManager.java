package com.webcrawler;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe manager for tracking visited URLs and pending URLs to crawl.
 */
public class UrlManager {
    private final Set<String> visitedUrls;
    private final ConcurrentLinkedQueue<UrlDepthPair> urlQueue;
    private final AtomicInteger crawledCount;
    private final String rootDomain;
    private final boolean stayInDomain;

    /**
     * Constructs a UrlManager.
     * 
     * @param rootUrl The starting URL
     * @param stayInDomain If true, only crawl URLs within the same domain
     */
    public UrlManager(String rootUrl, boolean stayInDomain) {
        this.visitedUrls = ConcurrentHashMap.newKeySet();
        this.urlQueue = new ConcurrentLinkedQueue<>();
        this.crawledCount = new AtomicInteger(0);
        this.stayInDomain = stayInDomain;
        this.rootDomain = extractDomain(rootUrl);
    }

    /**
     * Adds a URL to the queue if it hasn't been visited.
     * 
     * @param url The URL to add
     * @param depth The depth level of this URL
     * @return true if URL was added, false if already visited or outside domain
     */
    public boolean addUrl(String url, int depth) {
        if (url == null || url.isEmpty()) {
            return false;
        }

        // Normalize URL
        url = normalizeUrl(url);

        // Check domain restriction
        if (stayInDomain && !isSameDomain(url)) {
            return false;
        }

        // Check if already visited
        if (visitedUrls.contains(url)) {
            return false;
        }

        // Mark as visited and add to queue
        if (visitedUrls.add(url)) {
            urlQueue.offer(new UrlDepthPair(url, depth));
            return true;
        }

        return false;
    }

    /**
     * Gets the next URL to crawl from the queue.
     * 
     * @return The next UrlDepthPair or null if queue is empty
     */
    public UrlDepthPair getNextUrl() {
        return urlQueue.poll();
    }

    /**
     * Checks if there are more URLs to crawl.
     * 
     * @return true if queue is not empty
     */
    public boolean hasMoreUrls() {
        return !urlQueue.isEmpty();
    }

    /**
     * Increments and returns the count of crawled pages.
     * 
     * @return The new count of crawled pages
     */
    public int incrementCrawledCount() {
        return crawledCount.incrementAndGet();
    }

    /**
     * Gets the current count of crawled pages.
     * 
     * @return The number of pages crawled so far
     */
    public int getCrawledCount() {
        return crawledCount.get();
    }

    /**
     * Gets the total number of discovered URLs (visited + queued).
     * 
     * @return Total discovered URL count
     */
    public int getTotalDiscoveredUrls() {
        return visitedUrls.size();
    }

    /**
     * Gets the number of URLs in the queue.
     * 
     * @return Queue size
     */
    public int getQueueSize() {
        return urlQueue.size();
    }

    /**
     * Normalizes a URL by removing fragments and converting to lowercase.
     * 
     * @param url The URL to normalize
     * @return Normalized URL
     */
    private String normalizeUrl(String url) {
        // Remove fragment identifier
        int fragmentIndex = url.indexOf('#');
        if (fragmentIndex != -1) {
            url = url.substring(0, fragmentIndex);
        }

        // Remove trailing slash
        if (url.endsWith("/") && url.length() > 1) {
            url = url.substring(0, url.length() - 1);
        }

        return url.toLowerCase();
    }

    /**
     * Extracts the domain from a URL.
     * 
     * @param url The URL
     * @return The domain (e.g., "example.com")
     */
    private String extractDomain(String url) {
        try {
            String domain = url.toLowerCase();
            
            // Remove protocol
            if (domain.contains("://")) {
                domain = domain.substring(domain.indexOf("://") + 3);
            }
            
            // Remove path
            int slashIndex = domain.indexOf('/');
            if (slashIndex != -1) {
                domain = domain.substring(0, slashIndex);
            }
            
            // Remove port
            int colonIndex = domain.indexOf(':');
            if (colonIndex != -1) {
                domain = domain.substring(0, colonIndex);
            }
            
            return domain;
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Checks if a URL belongs to the same domain as the root.
     * 
     * @param url The URL to check
     * @return true if same domain
     */
    private boolean isSameDomain(String url) {
        String domain = extractDomain(url);
        return domain.equals(rootDomain);
    }

    /**
     * Inner class to hold URL and its depth level.
     */
    public static class UrlDepthPair {
        private final String url;
        private final int depth;

        public UrlDepthPair(String url, int depth) {
            this.url = url;
            this.depth = depth;
        }

        public String getUrl() {
            return url;
        }

        public int getDepth() {
            return depth;
        }
    }
}
