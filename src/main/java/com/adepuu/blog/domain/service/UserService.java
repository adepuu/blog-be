package com.adepuu.blog.domain.service;

import com.adepuu.blog.delivery.dto.user.UpdateProfileInput;
import com.adepuu.blog.domain.entity.User;

import java.util.UUID;

public interface UserService {
    int getFollowersCount(UUID userId);
    int getFollowingCount(UUID userId);
    int getPostsCount(UUID userId);
    User updateProfile(UUID userId, UpdateProfileInput input);
    void followUser(UUID followerId, UUID followedId);
    void unfollowUser(UUID followerId, UUID followedId);
}
