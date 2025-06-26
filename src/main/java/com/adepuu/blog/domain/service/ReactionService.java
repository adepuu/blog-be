package com.adepuu.blog.domain.service;

import com.adepuu.blog.domain.entity.Reaction;
import com.adepuu.blog.domain.entity.ReactionType;

import java.util.List;
import java.util.UUID;

public interface ReactionService {
    Reaction reactToPost(UUID postId, String reactionTypeName, String userId);
    boolean removeReactionFromPost(UUID postId, String reactionTypeName, String userId);
    Reaction reactToComment(UUID commentId, String reactionTypeName, String userId);
    boolean removeReactionFromComment(UUID commentId, String reactionTypeName, String userId);
    List<Reaction> getPostReactions(UUID postId);
    List<Reaction> getCommentReactions(UUID commentId);
    List<ReactionType> getAvailableReactionTypes();
    boolean hasUserReacted(UUID postId, UUID commentId, String reactionTypeName, String userId);
}
