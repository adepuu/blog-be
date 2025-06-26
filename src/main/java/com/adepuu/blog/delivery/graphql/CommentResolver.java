package com.adepuu.blog.delivery.graphql;

import com.adepuu.blog.delivery.dto.comment.CreateCommentInput;
import com.adepuu.blog.domain.entity.Comment;
import com.adepuu.blog.domain.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class CommentResolver {

    private final CommentService commentService;

    // Comment queries
    @QueryMapping("comments")
    public CommentConnection comments(
            @Argument("postId") String postId,
            @Argument("page") Integer page,
            @Argument("size") Integer size) {
        int pageNumber = page != null ? page : 0;
        int pageSize = size != null && size <= 100 ? size : 10;

        Page<Comment> commentPage = commentService.getCommentsByPost(
                UUID.fromString(postId), PageRequest.of(pageNumber, pageSize));

        return CommentConnection.builder()
                .nodes(commentPage.getContent())
                .pageInfo(PageInfo.builder()
                        .hasNextPage(commentPage.hasNext())
                        .hasPreviousPage(commentPage.hasPrevious())
                        .build())
                .totalCount((int) commentPage.getTotalElements())
                .build();
    }

    @QueryMapping("comment")
    public Comment comment(@Argument("id") String id) {
        return commentService.getComment(UUID.fromString(id));
    }

    // Comment field resolvers
    @SchemaMapping(typeName = "Comment", field = "repliesCount")
    public int repliesCount(Comment comment) {
        // TODO: Implement proper replies count
        return 0;
    }

    @SchemaMapping(typeName = "Comment", field = "reactionsCount")
    public int reactionsCount(Comment comment) {
        // TODO: Implement proper reactions count when reaction system is ready
        return 0;
    }

    // Comment mutations
    @MutationMapping("createComment")
    @PreAuthorize("hasRole('USER')")
    public Comment createComment(@Argument("input") CreateCommentInput input) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = (String) auth.getPrincipal();
        
        return commentService.createComment(input, userId);
    }

    @MutationMapping("updateComment")
    @PreAuthorize("hasRole('USER')")
    public Comment updateComment(@Argument("id") String id, @Argument("content") String content) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = (String) auth.getPrincipal();
        
        return commentService.updateComment(UUID.fromString(id), content, userId);
    }

    @MutationMapping("deleteComment")
    @PreAuthorize("hasRole('USER')")
    public Boolean deleteComment(@Argument("id") String id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = (String) auth.getPrincipal();
        
        return commentService.deleteComment(UUID.fromString(id), userId);
    }

    // Inner classes for GraphQL types
    @lombok.Data
    @lombok.Builder
    public static class CommentConnection {
        private List<Comment> nodes;
        private PageInfo pageInfo;
        private int totalCount;
    }

    @lombok.Data
    @lombok.Builder
    public static class PageInfo {
        private boolean hasNextPage;
        private boolean hasPreviousPage;
        private String startCursor;
        private String endCursor;
    }
}
