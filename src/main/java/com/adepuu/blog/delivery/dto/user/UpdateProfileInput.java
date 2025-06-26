package com.adepuu.blog.delivery.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public record UpdateProfileInput(
        @Size(max = 100, message = "Display name must not exceed 100 characters")
        String displayName,
        
        @Size(max = 1000, message = "Bio must not exceed 1000 characters")
        String bio,
        
        @Pattern(regexp = "^https?://.*", message = "Profile image URL must be a valid HTTP(S) URL")
        String profileImageUrl,
        
        @Pattern(regexp = "^https?://github\\.com/.*", message = "Must be a valid GitHub URL")
        String githubUrl,
        
        @Pattern(regexp = "^https?://(twitter\\.com|x\\.com)/.*", message = "Must be a valid Twitter/X URL")
        String twitterUrl,
        
        @Pattern(regexp = "^https?://.*", message = "Website URL must be a valid HTTP(S) URL")
        String websiteUrl,
        
        @Size(max = 100, message = "Location must not exceed 100 characters")
        String location,
        
        @Email(message = "Email must be valid")
        String email
) {
}
