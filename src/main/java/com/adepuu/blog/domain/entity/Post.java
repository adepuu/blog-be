package com.adepuu.blog.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "posts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Post {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, length = 300)
    private String title;
    
    @Column(unique = true, nullable = false, length = 350)
    private String slug;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Column(columnDefinition = "TEXT")
    private String excerpt;
    
    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;
    
    @Column(name = "canonical_url", length = 500)
    private String canonicalUrl;
    
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "post_status")
    @Builder.Default
    private PostStatus status = PostStatus.DRAFT;
    
    @Column(name = "reading_time_minutes")
    @Builder.Default
    private Integer readingTimeMinutes = 1;
    
    @Column(name = "views_count")
    @Builder.Default
    private Integer viewsCount = 0;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;
    
    @Column(name = "organization_id")
    private UUID organizationId;
    
    @Column(name = "published_at")
    private OffsetDateTime publishedAt;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
    
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
    
    public enum PostStatus {
        DRAFT, PUBLISHED, ARCHIVED
    }
}
