package com.adepuu.blog.delivery.graphql;

import com.adepuu.blog.delivery.dto.auth.AuthResponse;
import com.adepuu.blog.delivery.dto.auth.LoginRequest;
import com.adepuu.blog.delivery.dto.auth.RegisterRequest;
import com.adepuu.blog.domain.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class AuthResolver {
    
    private final AuthService authService;
    
    @MutationMapping("login")
    public AuthResponse login(@Argument("input") @Valid LoginRequest input) {
        return authService.login(input);
    }
    
    @MutationMapping("register")
    public AuthResponse register(@Argument("input") @Valid RegisterRequest input) {
        return authService.register(input);
    }
    
    @MutationMapping("refreshToken")
    public AuthResponse refreshToken(@Argument("refreshToken") String refreshToken) {
        return authService.refreshToken(refreshToken);
    }
    
    @MutationMapping("logout")
    public Boolean logout(@Argument("refreshToken") String refreshToken) {
        authService.logout(refreshToken);
        return true;
    }
}
