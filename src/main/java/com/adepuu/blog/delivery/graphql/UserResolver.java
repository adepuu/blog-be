package com.adepuu.blog.delivery.graphql;

import com.adepuu.blog.delivery.dto.user.UpdateProfileInput;
import com.adepuu.blog.domain.entity.User;
import com.adepuu.blog.domain.repository.UserRepository;
import com.adepuu.blog.domain.service.UserService;
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
public class UserResolver {

    private final UserRepository userRepository;
    private final UserService userService;

    @QueryMapping("me")
    @PreAuthorize("hasRole('USER')")
    public User me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = (String) auth.getPrincipal();
        return userRepository.findActiveById(UUID.fromString(userId)).orElse(null);
    }

    @QueryMapping("user")
    public User user(@Argument("username") String username) {
        return userRepository.findByUsernameAndIsActiveTrue(username).orElse(null);
    }

    @QueryMapping("users")
    public UserConnection users(
            @Argument("search") String search,
            @Argument("page") Integer page,
            @Argument("size") Integer size) {
        int pageNumber = page != null ? page : 0;
        int pageSize = size != null && size <= 100 ? size : 10;

        Page<User> userPage;
        if (search != null && !search.trim().isEmpty()) {
            userPage = userRepository.findByUsernameContainingIgnoreCaseAndIsActiveTrue(
                search.trim(), PageRequest.of(pageNumber, pageSize));
        } else {
            userPage = userRepository.findByIsActiveTrue(PageRequest.of(pageNumber, pageSize));
        }

        return UserConnection.builder()
                .nodes(userPage.getContent())
                .pageInfo(PageInfo.builder()
                        .hasNextPage(userPage.hasNext())
                        .hasPreviousPage(userPage.hasPrevious())
                        .build())
                .totalCount((int) userPage.getTotalElements())
                .build();
    }

    // User field resolvers
    @SchemaMapping(typeName = "User", field = "username")
    public String username(User user) {
        return user.getUsername();
    }

    @SchemaMapping(typeName = "User", field = "displayName")
    public String displayName(User user) {
        return user.getDisplayName();
    }

    @SchemaMapping(typeName = "User", field = "bio")
    public String bio(User user) {
        return user.getBio();
    }

    @SchemaMapping(typeName = "User", field = "profileImageUrl")
    public String profileImageUrl(User user) {
        return user.getProfileImageUrl();
    }

    @SchemaMapping(typeName = "User", field = "githubUrl")
    public String githubUrl(User user) {
        return user.getGithubUrl();
    }

    @SchemaMapping(typeName = "User", field = "twitterUrl")
    public String twitterUrl(User user) {
        return user.getTwitterUrl();
    }

    @SchemaMapping(typeName = "User", field = "websiteUrl")
    public String websiteUrl(User user) {
        return user.getWebsiteUrl();
    }

    @SchemaMapping(typeName = "User", field = "location")
    public String location(User user) {
        return user.getLocation();
    }

    @SchemaMapping(typeName = "User", field = "role")
    public User.UserRole role(User user) {
        return user.getRole();
    }

    @SchemaMapping(typeName = "User", field = "emailVerified")
    public Boolean emailVerified(User user) {
        return user.getEmailVerified();
    }

    @SchemaMapping(typeName = "User", field = "isActive")
    public Boolean isActive(User user) {
        return user.getIsActive();
    }

    @SchemaMapping(typeName = "User", field = "createdAt")
    public String createdAt(User user) {
        return user.getCreatedAt() != null ? user.getCreatedAt().toString() : null;
    }

    @SchemaMapping(typeName = "User", field = "updatedAt")
    public String updatedAt(User user) {
        return user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : null;
    }

    @SchemaMapping(typeName = "User", field = "followersCount")
    public int followersCount(User user) {
        return userService.getFollowersCount(user.getId());
    }

    @SchemaMapping(typeName = "User", field = "followingCount")
    public int followingCount(User user) {
        return userService.getFollowingCount(user.getId());
    }

    @SchemaMapping(typeName = "User", field = "postsCount")
    public int postsCount(User user) {
        return userService.getPostsCount(user.getId());
    }

    // User mutations
    @MutationMapping("updateProfile")
    @PreAuthorize("hasRole('USER')")
    public User updateProfile(@Argument("input") UpdateProfileInput input) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = (String) auth.getPrincipal();
        return userService.updateProfile(UUID.fromString(userId), input);
    }

    @MutationMapping("followUser")
    @PreAuthorize("hasRole('USER')")
    public Boolean followUser(@Argument("userId") String targetUserId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = (String) auth.getPrincipal();
        userService.followUser(UUID.fromString(currentUserId), UUID.fromString(targetUserId));
        return true;
    }

    @MutationMapping("unfollowUser")
    @PreAuthorize("hasRole('USER')")
    public Boolean unfollowUser(@Argument("userId") String targetUserId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = (String) auth.getPrincipal();
        userService.unfollowUser(UUID.fromString(currentUserId), UUID.fromString(targetUserId));
        return true;
    }

    // Inner classes for GraphQL types
    @lombok.Data
    @lombok.Builder
    public static class UserConnection {
        private List<User> nodes;
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
