package com.adepuu.blog.delivery.dto.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import java.util.List;

public record CreatePostInput(
        @NotBlank(message = "Title is required")
        @Size(min = 1, max = 300, message = "Title must be between 1 and 300 characters")
        String title,
        
        @NotBlank(message = "Content is required")
        @Size(min = 10, message = "Content must be at least 10 characters")
        String content,
        
        @Size(max = 500, message = "Excerpt must not exceed 500 characters")
        String excerpt,
        
        @Pattern(regexp = "^https?://.*", message = "Cover image URL must be a valid HTTP(S) URL")
        String coverImageUrl,
        
        @Pattern(regexp = "^https?://.*", message = "Canonical URL must be a valid HTTP(S) URL")
        String canonicalUrl,
        
        List<String> tagNames // Tag names to associate with the post
) {
}
