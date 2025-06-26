package com.adepuu.blog.infrastructure.service;

import com.adepuu.blog.delivery.dto.user.UpdateProfileInput;
import com.adepuu.blog.domain.entity.User;
import com.adepuu.blog.domain.repository.PostRepository;
import com.adepuu.blog.domain.repository.UserRepository;
import com.adepuu.blog.domain.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;

    @Override
    public int getFollowersCount(UUID userId) {
        // TODO: Implement when user following system is created
        return 0;
    }

    @Override
    public int getFollowingCount(UUID userId) {
        // TODO: Implement when user following system is created
        return 0;
    }

    @Override
    public int getPostsCount(UUID userId) {
        return (int) postRepository.countByAuthorIdAndDeletedAtIsNull(userId);
    }

    @Override
    @Transactional
    public User updateProfile(UUID userId, UpdateProfileInput input) {
        User user = userRepository.findActiveById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (input.displayName() != null) {
            user.setDisplayName(input.displayName());
        }
        if (input.bio() != null) {
            user.setBio(input.bio());
        }
        if (input.profileImageUrl() != null) {
            user.setProfileImageUrl(input.profileImageUrl());
        }
        if (input.githubUrl() != null) {
            user.setGithubUrl(input.githubUrl());
        }
        if (input.twitterUrl() != null) {
            user.setTwitterUrl(input.twitterUrl());
        }
        if (input.websiteUrl() != null) {
            user.setWebsiteUrl(input.websiteUrl());
        }
        if (input.location() != null) {
            user.setLocation(input.location());
        }
        if (input.email() != null) {
            user.setEmail(input.email());
        }

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void followUser(UUID followerId, UUID followedId) {
        // TODO: Implement when user following system is created
        log.info("User {} followed user {}", followerId, followedId);
    }

    @Override
    @Transactional
    public void unfollowUser(UUID followerId, UUID followedId) {
        // TODO: Implement when user following system is created
        log.info("User {} unfollowed user {}", followerId, followedId);
    }
}
