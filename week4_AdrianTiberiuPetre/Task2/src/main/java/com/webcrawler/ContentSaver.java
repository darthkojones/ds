package com.webcrawler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Handles saving crawled content to local files.
 */
public class ContentSaver {
    private static final Logger logger = LoggerFactory.getLogger(ContentSaver.class);
    private final File outputDirectory;
    private final DateTimeFormatter dateFormatter;

    /**
     * Constructs a ContentSaver.
     * 
     * @param outputPath The directory path where files will be saved
     */
    public ContentSaver(String outputPath) {
        this.outputDirectory = new File(outputPath);
        this.dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        // Create output directory if it doesn't exist
        if (!outputDirectory.exists()) {
            if (outputDirectory.mkdirs()) {
                logger.info("Created output directory: {}", outputDirectory.getAbsolutePath());
            } else {
                logger.error("Failed to create output directory: {}", outputDirectory.getAbsolutePath());
            }
        }
    }

    /**
     * Saves HTML content to a file with metadata.
     * 
     * @param url The URL of the page
     * @param content The HTML content
     * @param depth The crawl depth
     * @param title The page title
     * @return true if saved successfully
     */
    public boolean saveContent(String url, String content, int depth, String title) {
        try {
            String filename = generateFilename(url);
            File outputFile = new File(outputDirectory, filename);

            try (FileWriter writer = new FileWriter(outputFile, StandardCharsets.UTF_8)) {
                // Write metadata header
                writer.write("<!-- Crawled URL: " + url + " -->\n");
                writer.write("<!-- Crawl Time: " + LocalDateTime.now().format(dateFormatter) + " -->\n");
                writer.write("<!-- Depth Level: " + depth + " -->\n");
                writer.write("<!-- Page Title: " + escapeHtml(title) + " -->\n");
                writer.write("<!-- ================================================ -->\n\n");
                
                // Write actual content
                writer.write(content);
            }

            logger.info("Saved: {} (depth: {}) -> {}", url, depth, filename);
            return true;

        } catch (IOException e) {
            logger.error("Failed to save content from {}: {}", url, e.getMessage());
            return false;
        }
    }

    /**
     * Generates a safe filename from a URL using MD5 hash.
     * 
     * @param url The URL
     * @return A safe filename
     */
    private String generateFilename(String url) {
        try {
            // Use MD5 hash of URL to create unique, safe filename
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(url.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            
            return sb.toString() + ".html";
        } catch (Exception e) {
            // Fallback to simple sanitization
            String sanitized = url.replaceAll("[^a-zA-Z0-9]", "_");
            if (sanitized.length() > 100) {
                sanitized = sanitized.substring(0, 100);
            }
            return sanitized + ".html";
        }
    }

    /**
     * Escapes HTML special characters.
     * 
     * @param text The text to escape
     * @return Escaped text
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    /**
     * Creates a summary file with crawl statistics.
     * 
     * @param totalCrawled Total pages crawled
     * @param totalDiscovered Total URLs discovered
     * @param rootUrl The starting URL
     */
    public void createSummary(int totalCrawled, int totalDiscovered, String rootUrl) {
        try {
            File summaryFile = new File(outputDirectory, "crawl_summary.txt");
            
            try (FileWriter writer = new FileWriter(summaryFile, StandardCharsets.UTF_8)) {
                writer.write("Web Crawler Summary\n");
                writer.write("===================\n\n");
                writer.write("Crawl Time: " + LocalDateTime.now().format(dateFormatter) + "\n");
                writer.write("Root URL: " + rootUrl + "\n");
                writer.write("Total Pages Crawled: " + totalCrawled + "\n");
                writer.write("Total URLs Discovered: " + totalDiscovered + "\n");
                writer.write("Output Directory: " + outputDirectory.getAbsolutePath() + "\n");
            }

            logger.info("Created crawl summary: {}", summaryFile.getAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to create summary file: {}", e.getMessage());
        }
    }

    /**
     * Gets the output directory.
     * 
     * @return The output directory
     */
    public File getOutputDirectory() {
        return outputDirectory;
    }
}
