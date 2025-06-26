package com.adepuu.blog.delivery.graphql;

import com.adepuu.blog.delivery.dto.post.CreatePostInput;
import com.adepuu.blog.delivery.dto.post.UpdatePostInput;
import com.adepuu.blog.domain.entity.Post;
import com.adepuu.blog.domain.entity.Tag;
import com.adepuu.blog.domain.entity.User;
import com.adepuu.blog.domain.repository.PostRepository;
import com.adepuu.blog.domain.repository.UserRepository;
import com.adepuu.blog.domain.service.TagService;
import com.adepuu.blog.infrastructure.service.ContentSanitizationService;
import com.adepuu.blog.infrastructure.service.RateLimitingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class PostResolver {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final TagService tagService;
    private final ContentSanitizationService sanitizationService;
    private final RateLimitingService rateLimitingService;

    @QueryMapping("posts")
    public PostConnection posts(
            @Argument("page") Integer page,
            @Argument("size") Integer size) {
        int pageNumber = page != null ? page : 0;
        int pageSize = size != null && size <= 100 ? size : 10;

        Page<Post> postPage = postRepository.findPublishedPosts(PageRequest.of(pageNumber, pageSize));
        
        return PostConnection.builder()
                .nodes(postPage.getContent())
                .pageInfo(PostPageInfo.builder()
                        .hasNextPage(postPage.hasNext())
                        .hasPreviousPage(postPage.hasPrevious())
                        .build())
                .totalCount((int) postPage.getTotalElements())
                .build();
    }

    @QueryMapping("post")
    public Post getPost(@Argument("slug") String slug) {
        return postRepository.findBySlug(slug).orElse(null);
    }

    @QueryMapping("myPosts")
    @PreAuthorize("hasRole('USER')")
    public List<Post> getMyPosts(
            @Argument("page") Integer page,
            @Argument("size") Integer size) {
        String currentUserId = getCurrentUserId();
        User currentUser = userRepository.findActiveById(UUID.fromString(currentUserId))
                .orElseThrow(() -> new RuntimeException("User not found"));

        int pageNumber = page != null ? page : 0;
        int pageSize = size != null && size <= 100 ? size : 10;

        Page<Post> posts = postRepository.findByAuthor(currentUser, PageRequest.of(pageNumber, pageSize));
        return posts.getContent();
    }

    @MutationMapping("createPost")
    @PreAuthorize("hasRole('USER')")
    public Post createPost(@Argument("input") @Valid CreatePostInput input) {
        String currentUserId = getCurrentUserId();
        
        // Rate limiting check
        if (!rateLimitingService.isPostCreationAllowed(currentUserId)) {
            throw new RuntimeException("Rate limit exceeded for post creation");
        }
        
        User currentUser = userRepository.findActiveById(UUID.fromString(currentUserId))
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Content sanitization
        String sanitizedTitle = sanitizationService.sanitizeText(input.title());
        String sanitizedContent = sanitizationService.markdownToHtml(input.content());
        String sanitizedExcerpt = input.excerpt() != null ? 
            sanitizationService.sanitizeText(input.excerpt()) : 
            sanitizationService.extractExcerpt(input.content(), 200);
        
        // Profanity check
        if (sanitizationService.containsProfanity(sanitizedTitle) || 
            sanitizationService.containsProfanity(sanitizedContent)) {
            throw new RuntimeException("Content contains inappropriate language");
        }
        
        // URL validation
        if (input.coverImageUrl() != null && !sanitizationService.isValidUrl(input.coverImageUrl())) {
            throw new RuntimeException("Invalid cover image URL");
        }
        if (input.canonicalUrl() != null && !sanitizationService.isValidUrl(input.canonicalUrl())) {
            throw new RuntimeException("Invalid canonical URL");
        }
        
        String slug = sanitizationService.sanitizeSlug(sanitizedTitle);
        // Ensure slug uniqueness
        String finalSlug = ensureUniqueSlug(slug);
        
        // Get or create tags
        List<Tag> tags = input.tagNames() != null ? 
            tagService.getTagsByNames(input.tagNames()) : 
            List.of();

        Post post = Post.builder()
                .title(sanitizedTitle)
                .slug(finalSlug)
                .content(sanitizedContent)
                .excerpt(sanitizedExcerpt)
                .coverImageUrl(input.coverImageUrl())
                .canonicalUrl(input.canonicalUrl())
                .status(Post.PostStatus.DRAFT)
                .author(currentUser)
                .readingTimeMinutes(sanitizationService.calculateReadingTime(sanitizedContent))
                .tags(tags)
                .build();

        Post savedPost = postRepository.save(post);
        log.info("Post created successfully: {} by user: {}", savedPost.getId(), currentUserId);
        return savedPost;
    }

    @MutationMapping("publishPost")
    @PreAuthorize("hasRole('USER')")
    public Post publishPost(@Argument("id") String id) {
        Post post = postRepository.findActiveById(UUID.fromString(id))
                .orElseThrow(() -> new RuntimeException("Post not found"));

        String currentUserId = getCurrentUserId();

        // Users can only publish their own posts
        if (!post.getAuthor().getId().toString().equals(currentUserId)) {
            throw new RuntimeException("You can only publish your own posts");
        }

        post.setStatus(Post.PostStatus.PUBLISHED);
        post.setPublishedAt(OffsetDateTime.now());

        return postRepository.save(post);
    }

    @MutationMapping("deletePost")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public Boolean deletePost(@Argument("id") String id) {
        Post post = postRepository.findActiveById(UUID.fromString(id))
                .orElseThrow(() -> new RuntimeException("Post not found"));

        String currentUserId = getCurrentUserId();
        String currentUserRole = getCurrentUserRole();

        // Users can only delete their own posts, moderators and admins can delete any
        // post
        if (!currentUserRole.equals("MODERATOR") && !currentUserRole.equals("ADMIN")
                && !post.getAuthor().getId().toString().equals(currentUserId)) {
            throw new RuntimeException("You can only delete your own posts");
        }

        post.setDeletedAt(OffsetDateTime.now());
        postRepository.save(post);

        return true;
    }

    @MutationMapping("archivePost")
    @PreAuthorize("hasRole('MODERATOR') or hasRole('ADMIN')")
    public Post archivePost(@Argument("id") String id) {
        Post post = postRepository.findActiveById(UUID.fromString(id))
                .orElseThrow(() -> new RuntimeException("Post not found"));

        post.setStatus(Post.PostStatus.ARCHIVED);
        return postRepository.save(post);
    }

    // Add field resolvers for Post
    @SchemaMapping(typeName = "Post", field = "commentsCount")
    public int commentsCount(Post post) {
        // TODO: Implement when comment system is created
        return 0;
    }

    @SchemaMapping(typeName = "Post", field = "reactionsCount")
    public int reactionsCount(Post post) {
        // TODO: Implement when reaction system is created
        return 0;
    }

    @SchemaMapping(typeName = "Post", field = "isBookmarked")
    public boolean isBookmarked(Post post) {
        // TODO: Implement when bookmark system is created
        return false;
    }

    // Additional query mappings
    @QueryMapping("feed")
    @PreAuthorize("hasRole('USER')")
    public List<Post> feed(@Argument("page") Integer page, @Argument("size") Integer size) {
        // TODO: Implement personalized feed algorithm
        return getPostsList(page, size);
    }

    @QueryMapping("trendingPosts")
    public List<Post> trendingPosts(@Argument("timeframe") String timeframe) {
        // TODO: Implement trending algorithm based on views, reactions, etc.
        return getPostsList(0, 10);
    }

    // Additional mutation mappings
    @MutationMapping("updatePost")
    @PreAuthorize("hasRole('USER')")
    public Post updatePost(@Argument("id") String id, @Argument("input") UpdatePostInput input) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = (String) auth.getPrincipal();
        User user = userRepository.findActiveById(UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));

        Post post = postRepository.findActiveById(UUID.fromString(id))
                .orElseThrow(() -> new RuntimeException("Post not found"));

        // Verify ownership
        if (!post.getAuthor().getId().equals(user.getId())) {
            throw new RuntimeException("Not authorized to update this post");
        }

        // Update fields if provided
        if (input.title() != null) {
            post.setTitle(sanitizationService.sanitizeText(input.title()));
            post.setSlug(ensureUniqueSlug(generateSlug(input.title())));
        }
        if (input.content() != null) {
            String sanitizedContent = sanitizationService.markdownToHtml(input.content());
            post.setContent(sanitizedContent);
            post.setReadingTimeMinutes(calculateReadingTime(sanitizedContent));
        }
        if (input.excerpt() != null) {
            post.setExcerpt(sanitizationService.sanitizeText(input.excerpt()));
        }
        if (input.coverImageUrl() != null) {
            post.setCoverImageUrl(input.coverImageUrl());
        }
        if (input.canonicalUrl() != null) {
            post.setCanonicalUrl(input.canonicalUrl());
        }

        // Handle tags if provided
        if (input.tagNames() != null) {
            List<Tag> tags = tagService.getTagsByNames(input.tagNames());
            post.setTags(tags);
        }

        return postRepository.save(post);
    }

    @MutationMapping("addToReadingList")
    @PreAuthorize("hasRole('USER')")
    public Boolean addToReadingList(@Argument("postId") String postId) {
        // Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // String userId = (String) auth.getPrincipal();
        // TODO: Implement reading list functionality
        return true;
    }

    @MutationMapping("removeFromReadingList")
    @PreAuthorize("hasRole('USER')")
    public Boolean removeFromReadingList(@Argument("postId") String postId) {
        // Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // String userId = (String) auth.getPrincipal();
        // TODO: Implement reading list functionality
        return true;
    }

    // PostConnection field resolvers
    @SchemaMapping(typeName = "PostConnection", field = "nodes")
    public List<Post> postConnectionNodes(PostConnection postConnection) {
        return postConnection.getNodes();
    }

    @SchemaMapping(typeName = "PostConnection", field = "pageInfo")
    public PostPageInfo postConnectionPageInfo(PostConnection postConnection) {
        return postConnection.getPageInfo();
    }

    @SchemaMapping(typeName = "PostConnection", field = "totalCount")
    public int postConnectionTotalCount(PostConnection postConnection) {
        return postConnection.getTotalCount();
    }

    // Inner classes for GraphQL types
    @lombok.Data
    @lombok.Builder
    public static class PostConnection {
        private List<Post> nodes;
        private PostPageInfo pageInfo;
        private int totalCount;
    }

    @lombok.Data
    @lombok.Builder
    public static class PostPageInfo {
        private boolean hasNextPage;
        private boolean hasPreviousPage;
        private String startCursor;
        private String endCursor;
    }

    // Helper method
    private List<Post> getPostsList(Integer page, Integer size) {
        int pageNumber = page != null ? page : 0;
        int pageSize = size != null && size <= 100 ? size : 10;
        
        Page<Post> posts = postRepository.findPublishedPosts(PageRequest.of(pageNumber, pageSize));
        return posts.getContent();
    }

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        return authentication.getPrincipal().toString();
    }

    private String getCurrentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        return authentication.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
    }

    private String generateSlug(String title) {
        return title.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    private Integer calculateReadingTime(String content) {
        int wordCount = content.split("\\s+").length;
        int wordsPerMinute = 200;
        return Math.max(1, (int) Math.ceil((double) wordCount / wordsPerMinute));
    }
    
    private String ensureUniqueSlug(String baseSlug) {
        String slug = baseSlug;
        int counter = 1;
        
        while (postRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter;
            counter++;
        }
        
        return slug;
    }
}
