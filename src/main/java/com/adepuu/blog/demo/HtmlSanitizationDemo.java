package com.adepuu.blog.demo;

import com.adepuu.blog.infrastructure.service.ContentSanitizationService;

/**
 * Demo class showing OWASP HTML sanitization in action
 * Similar to how bluemonday works in Go
 */
public class HtmlSanitizationDemo {
    
    public static void main(String[] args) {
        ContentSanitizationService sanitizer = new ContentSanitizationService();
        
        System.out.println("=== HTML Sanitization Demo ===\n");
        
        // Example 1: Basic HTML sanitization
        String maliciousHtml = "<p>Safe content</p><script>alert('XSS')</script><img src='x' onerror='alert(1)'>";
        System.out.println("Original malicious HTML:");
        System.out.println(maliciousHtml);
        System.out.println("\nSanitized (basic policy):");
        System.out.println(sanitizer.sanitizeHtml(maliciousHtml));
        
        // Example 2: Blog content sanitization (more permissive)
        String blogContent = "<h1>Blog Title</h1><p>Content with <strong>bold</strong> text and <a href='https://example.com'>link</a></p><script>alert('xss')</script>";
        System.out.println("\n\nOriginal blog content:");
        System.out.println(blogContent);
        System.out.println("\nSanitized for blog (permissive policy):");
        System.out.println(sanitizer.sanitizeBlogContent(blogContent));
        
        // Example 3: Comment sanitization (restrictive)
        String commentContent = "<p>Comment with <strong>bold</strong></p><iframe src='evil.com'></iframe><script>alert('xss')</script>";
        System.out.println("\n\nOriginal comment:");
        System.out.println(commentContent);
        System.out.println("\nSanitized for comment (restrictive policy):");
        System.out.println(sanitizer.sanitizeCommentContent(commentContent));
        
        // Example 4: Plain text stripping
        String htmlWithText = "<p>This is <strong>important</strong> text with <script>alert('xss')</script> dangerous content</p>";
        System.out.println("\n\nOriginal HTML:");
        System.out.println(htmlWithText);
        System.out.println("\nStripped to plain text:");
        System.out.println(sanitizer.stripHtml(htmlWithText));
        
        // Example 5: Markdown to HTML with sanitization
        String markdown = "# Title\n\n**Bold text** and *italic*\n\n`code` and [link](https://example.com)\n\n<script>alert('xss')</script>";
        System.out.println("\n\nOriginal markdown:");
        System.out.println(markdown);
        System.out.println("\nConverted to HTML and sanitized:");
        System.out.println(sanitizer.markdownToHtml(markdown));
        
        // Example 6: Content type based sanitization
        String mixedContent = "<h2>Title</h2><p>Content with <script>alert('xss')</script> and <strong>formatting</strong></p>";
        System.out.println("\n\n=== Content Type Based Sanitization ===");
        System.out.println("Original content:");
        System.out.println(mixedContent);
        
        System.out.println("\nAs BLOG_POST:");
        System.out.println(sanitizer.sanitizeByContentType(mixedContent, ContentSanitizationService.ContentType.BLOG_POST));
        
        System.out.println("\nAs COMMENT:");
        System.out.println(sanitizer.sanitizeByContentType(mixedContent, ContentSanitizationService.ContentType.COMMENT));
        
        System.out.println("\nAs PLAIN_TEXT:");
        System.out.println(sanitizer.sanitizeByContentType(mixedContent, ContentSanitizationService.ContentType.PLAIN_TEXT));
    }
}
