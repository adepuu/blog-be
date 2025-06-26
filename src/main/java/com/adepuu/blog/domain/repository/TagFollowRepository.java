package com.adepuu.blog.domain.repository;

import com.adepuu.blog.domain.entity.TagFollow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TagFollowRepository extends JpaRepository<TagFollow, UUID> {
    
    Optional<TagFollow> findByTagIdAndUserId(UUID tagId, UUID userId);
    
    @Modifying
    @Query("UPDATE TagFollow tf SET tf.isFollowing = :isFollowing WHERE tf.tag.id = :tagId AND tf.user.id = :userId")
    int updateFollowStatus(@Param("tagId") UUID tagId, @Param("userId") UUID userId, @Param("isFollowing") boolean isFollowing);
    
    @Query("SELECT COUNT(tf) FROM TagFollow tf WHERE tf.tag.id = :tagId AND tf.isFollowing = true")
    long countActiveFollowersByTagId(@Param("tagId") UUID tagId);
}
