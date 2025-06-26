package com.adepuu.blog.domain.repository;

import com.adepuu.blog.domain.entity.ReactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReactionTypeRepository extends JpaRepository<ReactionType, UUID> {
    
    Optional<ReactionType> findByName(String name);
    
    boolean existsByName(String name);
}
