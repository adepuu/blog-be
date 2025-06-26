package com.adepuu.blog.infrastructure.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContentSanitizationServiceTest {

    private final ContentSanitizationService sanitizationService = new ContentSanitizationService();

    @Test
    void testBasicHtmlSanitization() {
        String maliciousInput = "<script>alert('XSS')</script><p>Safe content</p><img src='x' onerror='alert(1)'>";
        String result = sanitizationService.sanitizeHtml(maliciousInput);
        
        // Should remove script tags and dangerous attributes
        assertFalse(result.contains("<script>"));
        assertFalse(result.contains("onerror"));
        assertTrue(result.contains("Safe content"));
    }

    @Test
    void testBlogContentSanitization() {
        String blogContent = "<h1>Title</h1><p>Paragraph with <strong>bold</strong> and <em>italic</em></p>" +
                            "<a href='https://example.com'>Link</a>" +
                            "<script>alert('XSS')</script>";
        
        String result = sanitizationService.sanitizeBlogContent(blogContent);
        
        // Should preserve safe HTML elements
        assertTrue(result.contains("<h1>"));
        assertTrue(result.contains("<strong>"));
        assertTrue(result.contains("<em>"));
        assertTrue(result.contains("<a href=\"https://example.com\""));
        
        // Should remove dangerous elements
        assertFalse(result.contains("<script>"));
    }

    @Test
    void testCommentSanitization() {
        String comment = "<p>Comment with <strong>bold</strong></p>" +
                        "<script>alert('XSS')</script>" +
                        "<iframe src='evil.com'></iframe>";
        
        String result = sanitizationService.sanitizeCommentContent(comment);
        
        // Should preserve basic formatting
        assertTrue(result.contains("<strong>"));
        
        // Should remove dangerous elements
        assertFalse(result.contains("<script>"));
        assertFalse(result.contains("<iframe>"));
    }

    @Test
    void testMarkdownToHtml() {
        String markdown = "# Title\n\n**Bold text** and *italic text*\n\n`code` and ```code block```";
        String result = sanitizationService.markdownToHtml(markdown);
        
        assertTrue(result.contains("<h1>"));
        assertTrue(result.contains("<strong>"));
        assertTrue(result.contains("<em>"));
        assertTrue(result.contains("<code>"));
    }

    @Test
    void testUrlSanitization() {
        assertEquals("https://example.com", sanitizationService.sanitizeUrl("https://example.com"));
        assertEquals("http://example.com", sanitizationService.sanitizeUrl("http://example.com"));
        assertEquals("", sanitizationService.sanitizeUrl("javascript:alert('XSS')"));
        assertEquals("", sanitizationService.sanitizeUrl("ftp://example.com"));
        assertEquals("", sanitizationService.sanitizeUrl(""));
        assertEquals("", sanitizationService.sanitizeUrl(null));
    }

    @Test
    void testStripHtml() {
        String htmlContent = "<p>This is <strong>bold</strong> and <em>italic</em> text.</p>";
        String result = sanitizationService.stripHtml(htmlContent);
        
        assertEquals("This is bold and italic text.", result);
        assertFalse(result.contains("<"));
        assertFalse(result.contains(">"));
    }

    @Test
    void testProfanityDetection() {
        assertTrue(sanitizationService.containsProfanity("This contains spam content"));
        assertTrue(sanitizationService.containsProfanity("ABUSE in caps"));
        assertFalse(sanitizationService.containsProfanity("This is clean content"));
        assertFalse(sanitizationService.containsProfanity(""));
        assertFalse(sanitizationService.containsProfanity(null));
    }

    @Test
    void testSlugSanitization() {
        assertEquals("hello-world", sanitizationService.sanitizeSlug("Hello World!"));
        assertEquals("test-123", sanitizationService.sanitizeSlug("Test@#$ 123"));
        assertEquals("", sanitizationService.sanitizeSlug(""));
        assertEquals("", sanitizationService.sanitizeSlug(null));
    }

    @Test
    void testReadingTimeCalculation() {
        String content = "word ".repeat(200); // 200 words
        assertEquals(1, sanitizationService.calculateReadingTime(content));
        
        String longContent = "word ".repeat(400); // 400 words
        assertEquals(2, sanitizationService.calculateReadingTime(longContent));
        
        assertEquals(1, sanitizationService.calculateReadingTime(""));
        assertEquals(1, sanitizationService.calculateReadingTime(null));
    }

    @Test
    void testExcerptExtraction() {
        String content = "This is a long content that should be truncated at some point.";
        String excerpt = sanitizationService.extractExcerpt(content, 20);
        
        assertTrue(excerpt.length() <= 24); // 20 + "..." 
        assertTrue(excerpt.endsWith("..."));
        
        String shortContent = "Short";
        assertEquals("Short", sanitizationService.extractExcerpt(shortContent, 20));
    }

    @Test
    void testContentTypeSanitization() {
        String content = "<p>Test <script>alert('xss')</script> content</p>";
        
        // Different content types should apply different policies
        String blogResult = sanitizationService.sanitizeByContentType(content, 
            ContentSanitizationService.ContentType.BLOG_POST);
        String commentResult = sanitizationService.sanitizeByContentType(content, 
            ContentSanitizationService.ContentType.COMMENT);
        String plainResult = sanitizationService.sanitizeByContentType(content, 
            ContentSanitizationService.ContentType.PLAIN_TEXT);
        
        // All should remove the script tag
        assertFalse(blogResult.contains("<script>"));
        assertFalse(commentResult.contains("<script>"));
        assertFalse(plainResult.contains("<script>"));
        
        // Plain text should have no HTML at all
        assertFalse(plainResult.contains("<"));
        assertTrue(plainResult.contains("Test")); // But should contain the text content
    }

    @Test
    void testHtmlStripping() {
        String htmlContent = "<p>This is <strong>bold</strong> and <script>alert('xss')</script> content</p>";
        String strippedResult = sanitizationService.stripHtml(htmlContent);
        
        // Should remove all HTML tags including dangerous ones
        assertFalse(strippedResult.contains("<"));
        assertFalse(strippedResult.contains(">"));
        assertTrue(strippedResult.contains("This is bold"));
        assertFalse(strippedResult.contains("script"));
    }
}
