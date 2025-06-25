package com.adepuu.blog.delivery.graphql;

import com.adepuu.blog.domain.entity.Post;
import com.adepuu.blog.domain.entity.User;
import com.adepuu.blog.domain.repository.PostRepository;
import com.adepuu.blog.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class PostResolver {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @QueryMapping("posts")
    public List<Post> getPosts(
            @Argument("page") Integer page,
            @Argument("size") Integer size) {
        int pageNumber = page != null ? page : 0;
        int pageSize = size != null && size <= 100 ? size : 10;

        Page<Post> posts = postRepository.findPublishedPosts(PageRequest.of(pageNumber, pageSize));
        return posts.getContent();
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
    public Post createPost(@Argument("input") CreatePostInput input) {
        String currentUserId = getCurrentUserId();
        User currentUser = userRepository.findActiveById(UUID.fromString(currentUserId))
                .orElseThrow(() -> new RuntimeException("User not found"));

        Post post = Post.builder()
                .title(input.title())
                .slug(generateSlug(input.title()))
                .content(input.content())
                .excerpt(input.excerpt())
                .coverImageUrl(input.coverImageUrl())
                .status(Post.PostStatus.DRAFT)
                .author(currentUser)
                .readingTimeMinutes(calculateReadingTime(input.content()))
                .build();

        return postRepository.save(post);
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

    // Input record for creating posts
    public record CreatePostInput(
            String title,
            String content,
            String excerpt,
            String coverImageUrl) {
    }
}
