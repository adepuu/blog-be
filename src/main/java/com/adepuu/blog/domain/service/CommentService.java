package com.adepuu.blog.domain.service;

import com.adepuu.blog.delivery.dto.comment.CreateCommentInput;
import com.adepuu.blog.domain.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CommentService {
    Comment createComment(CreateCommentInput input, String authorId);
    Comment updateComment(UUID commentId, String content, String authorId);
    boolean deleteComment(UUID commentId, String authorId);
    Comment getComment(UUID commentId);
    Page<Comment> getCommentsByPost(UUID postId, Pageable pageable);
    Page<Comment> getReplies(UUID parentCommentId, Pageable pageable);
    boolean reportComment(UUID commentId, String reason, String reporterId);
}
