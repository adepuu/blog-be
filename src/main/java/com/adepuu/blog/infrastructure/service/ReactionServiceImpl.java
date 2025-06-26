package com.adepuu.blog.infrastructure.service;

import com.adepuu.blog.domain.entity.*;
import com.adepuu.blog.domain.repository.*;
import com.adepuu.blog.domain.service.ReactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReactionServiceImpl implements ReactionService {
    
    private final ReactionRepository reactionRepository;
    private final ReactionTypeRepository reactionTypeRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    
    @Override
    @Transactional
    public Reaction reactToPost(UUID postId, String reactionTypeName, String userId) {
        try {
            UUID userUuid = UUID.fromString(userId);
            
            // Validate entities exist
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new IllegalArgumentException("Post not found"));
            
            User user = userRepository.findById(userUuid)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            ReactionType reactionType = reactionTypeRepository.findByName(reactionTypeName)
                    .orElseThrow(() -> new IllegalArgumentException("Reaction type not found: " + reactionTypeName));
            
            // Check if user already reacted with same type
            reactionRepository.findByPostIdAndUserIdAndReactionTypeName(postId, userUuid, reactionTypeName)
                    .ifPresent(existingReaction -> {
                        throw new IllegalArgumentException("User already reacted to this post with " + reactionTypeName);
                    });
            
            // Create new reaction
            Reaction reaction = Reaction.builder()
                    .post(post)
                    .user(user)
                    .reactionType(reactionType)
                    .build();
            
            Reaction savedReaction = reactionRepository.save(reaction);
            log.info("User {} reacted to post {} with {}", userId, postId, reactionTypeName);
            
            return savedReaction;
        } catch (Exception e) {
            log.error("Error reacting to post: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to react to post", e);
        }
    }
    
    @Override
    @Transactional
    public boolean removeReactionFromPost(UUID postId, String reactionTypeName, String userId) {
        try {
            UUID userUuid = UUID.fromString(userId);
            
            Reaction reaction = reactionRepository.findByPostIdAndUserIdAndReactionTypeName(postId, userUuid, reactionTypeName)
                    .orElse(null);
            
            if (reaction == null) {
                return false; // No reaction to remove
            }
            
            reactionRepository.delete(reaction);
            log.info("User {} removed {} reaction from post {}", userId, reactionTypeName, postId);
            
            return true;
        } catch (Exception e) {
            log.error("Error removing reaction from post: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    @Transactional
    public Reaction reactToComment(UUID commentId, String reactionTypeName, String userId) {
        try {
            UUID userUuid = UUID.fromString(userId);
            
            // Validate entities exist
            Comment comment = commentRepository.findById(commentId)
                    .orElseThrow(() -> new IllegalArgumentException("Comment not found"));
            
            User user = userRepository.findById(userUuid)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            ReactionType reactionType = reactionTypeRepository.findByName(reactionTypeName)
                    .orElseThrow(() -> new IllegalArgumentException("Reaction type not found: " + reactionTypeName));
            
            // Check if user already reacted with same type
            reactionRepository.findByCommentIdAndUserIdAndReactionTypeName(commentId, userUuid, reactionTypeName)
                    .ifPresent(existingReaction -> {
                        throw new IllegalArgumentException("User already reacted to this comment with " + reactionTypeName);
                    });
            
            // Create new reaction
            Reaction reaction = Reaction.builder()
                    .comment(comment)
                    .user(user)
                    .reactionType(reactionType)
                    .build();
            
            Reaction savedReaction = reactionRepository.save(reaction);
            log.info("User {} reacted to comment {} with {}", userId, commentId, reactionTypeName);
            
            return savedReaction;
        } catch (Exception e) {
            log.error("Error reacting to comment: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to react to comment", e);
        }
    }
    
    @Override
    @Transactional
    public boolean removeReactionFromComment(UUID commentId, String reactionTypeName, String userId) {
        try {
            UUID userUuid = UUID.fromString(userId);
            
            Reaction reaction = reactionRepository.findByCommentIdAndUserIdAndReactionTypeName(commentId, userUuid, reactionTypeName)
                    .orElse(null);
            
            if (reaction == null) {
                return false; // No reaction to remove
            }
            
            reactionRepository.delete(reaction);
            log.info("User {} removed {} reaction from comment {}", userId, reactionTypeName, commentId);
            
            return true;
        } catch (Exception e) {
            log.error("Error removing reaction from comment: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Reaction> getPostReactions(UUID postId) {
        return reactionRepository.findByPostId(postId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Reaction> getCommentReactions(UUID commentId) {
        return reactionRepository.findByCommentId(commentId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ReactionType> getAvailableReactionTypes() {
        return reactionTypeRepository.findAll();
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean hasUserReacted(UUID postId, UUID commentId, String reactionTypeName, String userId) {
        try {
            UUID userUuid = UUID.fromString(userId);
            return reactionRepository.hasUserReacted(postId, commentId, userUuid, reactionTypeName);
        } catch (Exception e) {
            log.error("Error checking if user has reacted: {}", e.getMessage(), e);
            return false;
        }
    }
}
