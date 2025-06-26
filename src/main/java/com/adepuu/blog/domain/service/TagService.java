package com.adepuu.blog.domain.service;

import com.adepuu.blog.domain.entity.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface TagService {
    Tag createTag(String name, String description, String color);
    Tag getTagBySlug(String slug);
    Tag getTagById(UUID id);
    List<Tag> getTagsByNames(List<String> names);
    Page<Tag> searchTags(String searchTerm, Pageable pageable);
    List<Tag> getPopularTags(int limit);
    boolean followTag(UUID tagId, String userId);
    boolean unfollowTag(UUID tagId, String userId);
    boolean isUserFollowingTag(UUID tagId, String userId);
    List<Tag> getFollowedTagsByUser(String userId);
}
