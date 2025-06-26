package com.adepuu.blog.delivery.graphql;

import com.adepuu.blog.domain.entity.Reaction;
import com.adepuu.blog.domain.entity.ReactionType;
import com.adepuu.blog.domain.service.ReactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ReactionResolver {

    private final ReactionService reactionService;

    // Reaction queries
    @QueryMapping("reactionTypes")
    public List<ReactionType> reactionTypes() {
        return reactionService.getAvailableReactionTypes();
    }

    @QueryMapping("postReactions")
    public List<Reaction> postReactions(@Argument("postId") String postId) {
        return reactionService.getPostReactions(UUID.fromString(postId));
    }

    @QueryMapping("commentReactions")
    public List<Reaction> commentReactions(@Argument("commentId") String commentId) {
        return reactionService.getCommentReactions(UUID.fromString(commentId));
    }

    // Reaction mutations
    @MutationMapping("reactToPost")
    @PreAuthorize("hasRole('USER')")
    public Reaction reactToPost(@Argument("postId") String postId, @Argument("reactionType") String reactionType) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = (String) auth.getPrincipal();
        
        return reactionService.reactToPost(UUID.fromString(postId), reactionType, userId);
    }

    @MutationMapping("removeReactionFromPost")
    @PreAuthorize("hasRole('USER')")
    public Boolean removeReactionFromPost(@Argument("postId") String postId, @Argument("reactionType") String reactionType) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = (String) auth.getPrincipal();
        
        return reactionService.removeReactionFromPost(UUID.fromString(postId), reactionType, userId);
    }

    @MutationMapping("reactToComment")
    @PreAuthorize("hasRole('USER')")
    public Reaction reactToComment(@Argument("commentId") String commentId, @Argument("reactionType") String reactionType) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = (String) auth.getPrincipal();
        
        return reactionService.reactToComment(UUID.fromString(commentId), reactionType, userId);
    }

    @MutationMapping("removeReactionFromComment")
    @PreAuthorize("hasRole('USER')")
    public Boolean removeReactionFromComment(@Argument("commentId") String commentId, @Argument("reactionType") String reactionType) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = (String) auth.getPrincipal();
        
        return reactionService.removeReactionFromComment(UUID.fromString(commentId), reactionType, userId);
    }
}
