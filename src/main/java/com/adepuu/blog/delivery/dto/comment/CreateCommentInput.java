package com.adepuu.blog.delivery.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentInput(
        @NotBlank(message = "Post ID is required")
        String postId,
        
        @NotBlank(message = "Content is required")
        @Size(min = 1, max = 2000, message = "Comment must be between 1 and 2000 characters")
        String content,
        
        String parentCommentId // Optional for replies
) {
}
