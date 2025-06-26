package com.adepuu.blog.domain.repository;

import com.adepuu.blog.domain.entity.Reaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReactionRepository extends JpaRepository<Reaction, UUID> {
    
    // Post reactions
    List<Reaction> findByPostId(UUID postId);
    
    @Query("SELECT r FROM Reaction r WHERE r.post.id = :postId AND r.user.id = :userId AND r.reactionType.name = :reactionTypeName")
    Optional<Reaction> findByPostIdAndUserIdAndReactionTypeName(
            @Param("postId") UUID postId, 
            @Param("userId") UUID userId, 
            @Param("reactionTypeName") String reactionTypeName);
    
    // Comment reactions
    List<Reaction> findByCommentId(UUID commentId);
    
    @Query("SELECT r FROM Reaction r WHERE r.comment.id = :commentId AND r.user.id = :userId AND r.reactionType.name = :reactionTypeName")
    Optional<Reaction> findByCommentIdAndUserIdAndReactionTypeName(
            @Param("commentId") UUID commentId, 
            @Param("userId") UUID userId, 
            @Param("reactionTypeName") String reactionTypeName);
    
    // Check if user has reacted
    @Query("SELECT COUNT(r) > 0 FROM Reaction r WHERE " +
           "(:postId IS NULL OR r.post.id = :postId) AND " +
           "(:commentId IS NULL OR r.comment.id = :commentId) AND " +
           "r.user.id = :userId AND r.reactionType.name = :reactionTypeName")
    boolean hasUserReacted(
            @Param("postId") UUID postId,
            @Param("commentId") UUID commentId,
            @Param("userId") UUID userId,
            @Param("reactionTypeName") String reactionTypeName);
    
    // Count reactions by type
    @Query("SELECT COUNT(r) FROM Reaction r WHERE r.post.id = :postId AND r.reactionType.name = :reactionTypeName")
    long countPostReactionsByType(@Param("postId") UUID postId, @Param("reactionTypeName") String reactionTypeName);
    
    @Query("SELECT COUNT(r) FROM Reaction r WHERE r.comment.id = :commentId AND r.reactionType.name = :reactionTypeName")
    long countCommentReactionsByType(@Param("commentId") UUID commentId, @Param("reactionTypeName") String reactionTypeName);
}
