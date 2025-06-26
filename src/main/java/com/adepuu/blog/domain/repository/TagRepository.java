package com.adepuu.blog.domain.repository;

import com.adepuu.blog.domain.entity.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TagRepository extends JpaRepository<Tag, UUID> {
    
    Optional<Tag> findBySlug(String slug);
    
    Optional<Tag> findByName(String name);
    
    List<Tag> findByNameIn(List<String> names);
    
    @Query("SELECT t FROM Tag t WHERE t.deletedAt IS NULL AND t.name IN :names")
    List<Tag> findActiveByNameIn(@Param("names") List<String> names);
    
    @Query("SELECT t FROM Tag t WHERE t.deletedAt IS NULL AND " +
           "(LOWER(t.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Tag> searchTags(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    @Query("SELECT t FROM Tag t WHERE t.deletedAt IS NULL ORDER BY t.postsCount DESC, t.followersCount DESC")
    List<Tag> findPopularTags(Pageable pageable);
    
    @Query("SELECT t FROM Tag t WHERE t.deletedAt IS NULL AND t.isOfficial = true ORDER BY t.name")
    List<Tag> findOfficialTags();
    
    @Query("SELECT DISTINCT t FROM Tag t " +
           "JOIN t.followers tf " +
           "WHERE t.deletedAt IS NULL AND tf.user.id = :userId AND tf.isFollowing = true " +
           "ORDER BY tf.createdAt DESC")
    List<Tag> findFollowedTagsByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT COUNT(tf) > 0 FROM TagFollow tf " +
           "WHERE tf.tag.id = :tagId AND tf.user.id = :userId AND tf.isFollowing = true")
    boolean isUserFollowingTag(@Param("tagId") UUID tagId, @Param("userId") UUID userId);
}
