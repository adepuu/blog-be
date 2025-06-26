package com.adepuu.blog.infrastructure.service;

import lombok.extern.slf4j.Slf4j;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Slf4j
@Service
public class ContentSanitizationService {

    // OWASP HTML Sanitizer policies
    private static final PolicyFactory BASIC_POLICY = Sanitizers.FORMATTING
            .and(Sanitizers.LINKS)
            .and(Sanitizers.BLOCKS)
            .and(Sanitizers.TABLES);

    // Custom policy for blog content (more permissive)
    private static final PolicyFactory BLOG_CONTENT_POLICY = new HtmlPolicyBuilder()
            .allowElements("p", "br", "div", "span", "h1", "h2", "h3", "h4", "h5", "h6",
                    "strong", "b", "em", "i", "u", "s", "strike", "del",
                    "ul", "ol", "li", "blockquote", "pre", "code",
                    "a", "img", "table", "thead", "tbody", "tr", "th", "td")
            .allowAttributes("href").onElements("a")
            .allowAttributes("src", "alt", "title", "width", "height").onElements("img")
            .allowAttributes("class").onElements("code", "pre", "blockquote")
            .requireRelNofollowOnLinks()
            .allowUrlProtocols("http", "https", "mailto")
            .toFactory();

    // Strict policy for comments (more restrictive)
    private static final PolicyFactory COMMENT_POLICY = new HtmlPolicyBuilder()
            .allowElements("p", "br", "strong", "b", "em", "i", "code", "pre", "blockquote", "a")
            .allowAttributes("href").onElements("a")
            .requireRelNofollowOnLinks()
            .allowUrlProtocols("http", "https")
            .toFactory();

    private static final Pattern PROFANITY_PATTERN = Pattern.compile(
            "\\b(spam|abuse|offensive)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");

    /**
     * Basic HTML sanitization using OWASP policy
     * Uses a basic policy suitable for general content
     */
    public String sanitizeHtml(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "";
        }

        try {
            return BASIC_POLICY.sanitize(content);
        } catch (Exception e) {
            log.warn("Failed to sanitize HTML content: {}", e.getMessage());
            // Fallback to tag removal
            return HTML_TAG_PATTERN.matcher(content).replaceAll("");
        }
    }

    /**
     * Sanitizes HTML content for blog posts
     * Uses a more permissive policy allowing rich formatting
     */
    public String sanitizeBlogContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "";
        }

        try {
            return BLOG_CONTENT_POLICY.sanitize(content);
        } catch (Exception e) {
            log.warn("Failed to sanitize blog content: {}", e.getMessage());
            return sanitizeHtml(content); // Fallback to basic sanitization
        }
    }

    /**
     * Sanitizes HTML content for comments
     * Uses a restrictive policy allowing only basic formatting
     */
    public String sanitizeCommentContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "";
        }

        try {
            return COMMENT_POLICY.sanitize(content);
        } catch (Exception e) {
            log.warn("Failed to sanitize comment content: {}", e.getMessage());
            return sanitizeHtml(content); // Fallback to basic sanitization
        }
    }

    /**
     * Basic markdown to HTML conversion with proper sanitization
     * TODO: Consider using a proper markdown library like flexmark
     */
    public String markdownToHtml(String markdown) {
        if (markdown == null || markdown.trim().isEmpty()) {
            return "";
        }

        try {
            // Simple markdown to HTML conversion
            String html = markdown
                    .replaceAll("\\*\\*(.*?)\\*\\*", "<strong>$1</strong>") // Bold
                    .replaceAll("\\*(.*?)\\*", "<em>$1</em>") // Italic
                    .replaceAll("\\n\\n", "</p><p>") // Paragraphs
                    .replaceAll("\\n", "<br>") // Line breaks
                    .replaceAll("```(.*?)```", "<pre><code>$1</code></pre>") // Code blocks
                    .replaceAll("`(.*?)`", "<code>$1</code>") // Inline code
                    .replaceAll("^#{1}\\s+(.*?)$", "<h1>$1</h1>") // H1
                    .replaceAll("^#{2}\\s+(.*?)$", "<h2>$1</h2>") // H2
                    .replaceAll("^#{3}\\s+(.*?)$", "<h3>$1</h3>"); // H3

            // Wrap in paragraph tags if not already wrapped
            if (!html.startsWith("<")) {
                html = "<p>" + html + "</p>";
            }

            return sanitizeBlogContent(html);
        } catch (Exception e) {
            log.warn("Failed to convert markdown to HTML: {}", e.getMessage());
            return sanitizeText(markdown);
        }
    }

    /**
     * Sanitizes plain text input
     */
    public String sanitizeText(String text) {
        if (text == null) {
            return "";
        }

        String sanitized = text.trim()
                .replaceAll("[\\r\\n]+", " ")
                .replaceAll("\\s+", " ");

        // Limit length
        if (sanitized.length() > 10000) {
            sanitized = sanitized.substring(0, 10000);
        }

        return sanitized;
    }

    /**
     * Basic profanity detection
     */
    public boolean containsProfanity(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }

        return PROFANITY_PATTERN.matcher(content).find();
    }

    /**
     * Validates if a URL is safe
     */
    public boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return true; // Allow empty URLs
        }

        return url.startsWith("http://") || url.startsWith("https://");
    }

    /**
     * Sanitizes and validates a slug
     */
    public String sanitizeSlug(String slug) {
        if (slug == null || slug.trim().isEmpty()) {
            return "";
        }

        String sanitized = slug.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        // Limit length
        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 100);
        }

        return sanitized;
    }

    /**
     * Calculates reading time for content
     */
    public int calculateReadingTime(String content) {
        if (content == null || content.trim().isEmpty()) {
            return 1;
        }

        String text = HTML_TAG_PATTERN.matcher(content).replaceAll("") // Remove HTML tags
                .replaceAll("\\s+", " "); // Normalize whitespace

        int wordCount = text.split("\\s+").length;
        int readingTime = Math.max(1, (int) Math.ceil(wordCount / 200.0)); // 200 words per minute

        return Math.min(readingTime, 60); // Cap at 60 minutes
    }

    /**
     * Extracts excerpt from content
     */
    public String extractExcerpt(String content, int maxLength) {
        if (content == null || content.trim().isEmpty()) {
            return "";
        }

        String text = HTML_TAG_PATTERN.matcher(content).replaceAll("") // Remove HTML tags
                .replaceAll("\\s+", " ") // Normalize whitespace
                .trim();

        if (text.length() <= maxLength) {
            return text;
        }

        String excerpt = text.substring(0, maxLength);
        int lastSpace = excerpt.lastIndexOf(" ");

        if (lastSpace > maxLength * 0.8) { // If last space is reasonably close to the end
            excerpt = excerpt.substring(0, lastSpace);
        }

        return excerpt + "...";
    }

    /**
     * Validates and sanitizes a URL for safety
     * Ensures only safe protocols are allowed
     */
    public String sanitizeUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return "";
        }

        String trimmedUrl = url.trim();

        // Only allow http and https protocols
        if (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://")) {
            return "";
        }

        // Basic URL validation - could be enhanced with more sophisticated checks
        try {
            java.net.URI.create(trimmedUrl);
            return trimmedUrl;
        } catch (Exception e) {
            log.warn("Invalid URL provided: {}", trimmedUrl);
            return "";
        }
    }

    /**
     * Strips all HTML tags and returns plain text
     * Useful for generating excerpts or search indexing
     */
    public String stripHtml(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "";
        }

        return HTML_TAG_PATTERN.matcher(content).replaceAll("")
                .replaceAll("&[a-zA-Z0-9#]+;", "") // Remove HTML entities
                .replaceAll("\\s+", " ") // Normalize whitespace
                .trim();
    }

    /**
     * Determines the appropriate sanitization method based on content type
     */
    public String sanitizeByContentType(String content, ContentType contentType) {
        if (content == null) {
            return "";
        }

        return switch (contentType) {
            case BLOG_POST -> sanitizeBlogContent(content);
            case COMMENT -> sanitizeCommentContent(content);
            case PLAIN_TEXT -> stripHtml(content);
            case MARKDOWN -> markdownToHtml(content);
            default -> sanitizeHtml(content);
        };
    }

    /**
     * Content types for different sanitization policies
     */
    public enum ContentType {
        BLOG_POST,
        COMMENT,
        PLAIN_TEXT,
        MARKDOWN,
        BASIC_HTML
    }
}
