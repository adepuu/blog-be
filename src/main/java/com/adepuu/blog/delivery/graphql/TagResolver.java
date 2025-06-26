package com.adepuu.blog.delivery.graphql;

import com.adepuu.blog.domain.entity.Tag;
import com.adepuu.blog.domain.service.TagService;
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
public class TagResolver {

    private final TagService tagService;

    // Tag queries
    @QueryMapping("tags")
    public List<Tag> tags(
            @Argument("search") String search,
            @Argument("page") Integer page,
            @Argument("size") Integer size) {
        int pageNumber = page != null ? page : 0;
        int pageSize = size != null && size <= 100 ? size : 10;

        Page<Tag> tagPage;
        if (search != null && !search.trim().isEmpty()) {
            tagPage = tagService.searchTags(search.trim(), PageRequest.of(pageNumber, pageSize));
        } else {
            tagPage = tagService.searchTags("", PageRequest.of(pageNumber, pageSize));
        }

        return tagPage.getContent();
    }

    @QueryMapping("tag")
    public Tag tag(@Argument("slug") String slug) {
        return tagService.getTagBySlug(slug);
    }

    @QueryMapping("popularTags")
    public List<Tag> popularTags() {
        return tagService.getPopularTags(20);
    }

    @QueryMapping("myFollowedTags")
    @PreAuthorize("hasRole('USER')")
    public List<Tag> myFollowedTags() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = (String) auth.getPrincipal();
        return tagService.getFollowedTagsByUser(userId);
    }

    // Tag field resolvers
    @SchemaMapping(typeName = "Tag", field = "isFollowing")
    public Boolean isFollowing(Tag tag) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return false;
            }
            String userId = (String) auth.getPrincipal();
            return tagService.isUserFollowingTag(tag.getId(), userId);
        } catch (Exception e) {
            return false;
        }
    }

    // Tag mutations
    @MutationMapping("followTag")
    @PreAuthorize("hasRole('USER')")
    public Boolean followTag(@Argument("tagId") String tagId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = (String) auth.getPrincipal();
        return tagService.followTag(UUID.fromString(tagId), userId);
    }

    @MutationMapping("unfollowTag")
    @PreAuthorize("hasRole('USER')")
    public Boolean unfollowTag(@Argument("tagId") String tagId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = (String) auth.getPrincipal();
        return tagService.unfollowTag(UUID.fromString(tagId), userId);
    }

    @MutationMapping("createTag")
    @PreAuthorize("hasRole('MODERATOR') or hasRole('ADMIN')")
    public Tag createTag(
            @Argument("name") String name,
            @Argument("description") String description,
            @Argument("color") String color) {
        return tagService.createTag(name, description, color);
    }
}
