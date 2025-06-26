package com.adepuu.blog.infrastructure.service;

import com.adepuu.blog.domain.entity.Tag;
import com.adepuu.blog.domain.entity.TagFollow;
import com.adepuu.blog.domain.entity.User;
import com.adepuu.blog.domain.repository.TagFollowRepository;
import com.adepuu.blog.domain.repository.TagRepository;
import com.adepuu.blog.domain.repository.UserRepository;
import com.adepuu.blog.domain.service.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {
    
    private final TagRepository tagRepository;
    private final TagFollowRepository tagFollowRepository;
    private final UserRepository userRepository;
    
    @Override
    @Transactional
    public Tag createTag(String name, String description, String color) {
        // Check if tag already exists
        if (tagRepository.findByName(name).isPresent()) {
            throw new IllegalArgumentException("Tag with name '" + name + "' already exists");
        }
        
        // Generate slug from name
        String slug = generateSlug(name);
        
        // Ensure slug is unique
        if (tagRepository.findBySlug(slug).isPresent()) {
            slug = slug + "-" + System.currentTimeMillis();
        }
        
        Tag tag = Tag.builder()
                .name(name)
                .slug(slug)
                .description(description)
                .color(color != null ? color : "#000000")
                .backgroundColor("#FFFFFF")
                .isOfficial(false)
                .postsCount(0)
                .followersCount(0)
                .build();
        
        Tag savedTag = tagRepository.save(tag);
        log.info("Created new tag: {} with slug: {}", name, slug);
        return savedTag;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Tag getTagBySlug(String slug) {
        return tagRepository.findBySlug(slug).orElse(null);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Tag getTagById(UUID id) {
        return tagRepository.findById(id).orElse(null);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Tag> getTagsByNames(List<String> names) {
        return tagRepository.findActiveByNameIn(names);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<Tag> searchTags(String searchTerm, Pageable pageable) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return tagRepository.findAll(pageable);
        }
        return tagRepository.searchTags(searchTerm.trim(), pageable);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Tag> getPopularTags(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return tagRepository.findPopularTags(pageable);
    }
    
    @Override
    @Transactional
    public boolean followTag(UUID tagId, String userId) {
        try {
            UUID userUuid = UUID.fromString(userId);
            
            // Check if tag exists
            Tag tag = tagRepository.findById(tagId)
                    .orElseThrow(() -> new IllegalArgumentException("Tag not found"));
            
            // Check if user exists
            User user = userRepository.findById(userUuid)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            // Check if already following
            TagFollow existingFollow = tagFollowRepository.findByTagIdAndUserId(tagId, userUuid)
                    .orElse(null);
            
            if (existingFollow != null) {
                if (existingFollow.getIsFollowing()) {
                    return false; // Already following
                } else {
                    // Update existing record
                    existingFollow.setIsFollowing(true);
                    tagFollowRepository.save(existingFollow);
                }
            } else {
                // Create new follow record
                TagFollow tagFollow = TagFollow.builder()
                        .tag(tag)
                        .user(user)
                        .isFollowing(true)
                        .build();
                tagFollowRepository.save(tagFollow);
            }
            
            // Update followers count
            updateTagFollowersCount(tagId);
            
            log.info("User {} started following tag {}", userId, tag.getName());
            return true;
        } catch (Exception e) {
            log.error("Error following tag: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    @Transactional
    public boolean unfollowTag(UUID tagId, String userId) {
        try {
            UUID userUuid = UUID.fromString(userId);
            
            // Check if following relationship exists
            TagFollow tagFollow = tagFollowRepository.findByTagIdAndUserId(tagId, userUuid)
                    .orElse(null);
            
            if (tagFollow == null || !tagFollow.getIsFollowing()) {
                return false; // Not following
            }
            
            // Update follow status
            tagFollow.setIsFollowing(false);
            tagFollowRepository.save(tagFollow);
            
            // Update followers count
            updateTagFollowersCount(tagId);
            
            log.info("User {} unfollowed tag {}", userId, tagId);
            return true;
        } catch (Exception e) {
            log.error("Error unfollowing tag: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean isUserFollowingTag(UUID tagId, String userId) {
        try {
            UUID userUuid = UUID.fromString(userId);
            return tagRepository.isUserFollowingTag(tagId, userUuid);
        } catch (Exception e) {
            log.error("Error checking if user is following tag: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Tag> getFollowedTagsByUser(String userId) {
        try {
            UUID userUuid = UUID.fromString(userId);
            return tagRepository.findFollowedTagsByUserId(userUuid);
        } catch (Exception e) {
            log.error("Error getting followed tags for user: {}", e.getMessage(), e);
            return List.of();
        }
    }
    
    @Transactional
    public void updateTagFollowersCount(UUID tagId) {
        try {
            long followersCount = tagFollowRepository.countActiveFollowersByTagId(tagId);
            tagRepository.findById(tagId).ifPresent(tag -> {
                tag.setFollowersCount((int) followersCount);
                tagRepository.save(tag);
            });
        } catch (Exception e) {
            log.error("Error updating tag followers count: {}", e.getMessage(), e);
        }
    }
    
    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "") // Remove special characters except spaces and hyphens
                .replaceAll("\\s+", "-") // Replace spaces with hyphens
                .replaceAll("-+", "-") // Replace multiple hyphens with single hyphen
                .replaceAll("^-|-$", ""); // Remove leading/trailing hyphens
    }
}
