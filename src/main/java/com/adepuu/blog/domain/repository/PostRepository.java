package com.adepuu.blog.domain.repository;

import com.adepuu.blog.domain.entity.Post;
import com.adepuu.blog.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {
    
    @Query("SELECT p FROM Post p WHERE p.status = 'PUBLISHED' AND p.deletedAt IS NULL ORDER BY p.publishedAt DESC")
    Page<Post> findPublishedPosts(Pageable pageable);
    
    @Query("SELECT p FROM Post p WHERE p.author = :author AND p.deletedAt IS NULL ORDER BY p.createdAt DESC")
    Page<Post> findByAuthor(User author, Pageable pageable);
    
    @Query("SELECT p FROM Post p WHERE p.slug = :slug AND p.deletedAt IS NULL")
    Optional<Post> findBySlug(String slug);
    
    @Query("SELECT p FROM Post p WHERE p.id = :id AND p.deletedAt IS NULL")
    Optional<Post> findActiveById(UUID id);
}
