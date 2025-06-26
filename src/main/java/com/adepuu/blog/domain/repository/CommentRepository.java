package com.adepuu.blog.domain.repository;

import com.adepuu.blog.domain.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {
    
    @Query("SELECT c FROM Comment c WHERE c.deletedAt IS NULL AND c.id = :id")
    Optional<Comment> findActiveById(@Param("id") UUID id);
    
    @Query("SELECT c FROM Comment c WHERE c.deletedAt IS NULL AND c.post.id = :postId ORDER BY c.createdAt ASC")
    List<Comment> findActiveByPostId(@Param("postId") UUID postId);
    
    @Query("SELECT c FROM Comment c WHERE c.deletedAt IS NULL AND c.post.id = :postId")
    Page<Comment> findActiveByPostId(@Param("postId") UUID postId, Pageable pageable);
    
    @Query("SELECT c FROM Comment c WHERE c.deletedAt IS NULL AND c.parentComment.id = :parentId ORDER BY c.createdAt ASC")
    List<Comment> findRepliesByParentId(@Param("parentId") UUID parentId);
    
    @Query("SELECT c FROM Comment c WHERE c.deletedAt IS NULL AND c.author.id = :authorId ORDER BY c.createdAt DESC")
    Page<Comment> findActiveByAuthorId(@Param("authorId") UUID authorId, Pageable pageable);
    
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.deletedAt IS NULL AND c.post.id = :postId")
    long countActiveByPostId(@Param("postId") UUID postId);
    
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.deletedAt IS NULL AND c.parentComment.id = :parentId")
    long countRepliesByParentId(@Param("parentId") UUID parentId);
}
