package com.adepuu.blog.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tags")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tag {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(unique = true, nullable = false, length = 50)
    private String name;
    
    @Column(unique = true, nullable = false, length = 50)
    private String slug;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(length = 7)
    @Builder.Default
    private String color = "#000000";
    
    @Column(name = "background_color", length = 7)
    @Builder.Default
    private String backgroundColor = "#FFFFFF";
    
    @Column(name = "is_official")
    @Builder.Default
    private Boolean isOfficial = false;
    
    @Column(name = "posts_count")
    @Builder.Default
    private Integer postsCount = 0;
    
    @Column(name = "followers_count")
    @Builder.Default
    private Integer followersCount = 0;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
    
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
    
    // Relationships
    @ManyToMany(mappedBy = "tags")
    private List<Post> posts;
    
    @OneToMany(mappedBy = "tag", cascade = CascadeType.ALL)
    private List<TagFollow> followers;
}
