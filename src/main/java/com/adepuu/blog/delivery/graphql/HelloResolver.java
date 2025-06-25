package com.adepuu.blog.delivery.graphql;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.adepuu.blog.delivery.dto.SampleResponse;
import com.adepuu.blog.delivery.dto.UserRegisterRequest;
import com.adepuu.blog.delivery.dto.UserResponse;

@Controller
public class HelloResolver {

    Map<String, UserResponse> userDatabase = new HashMap<>();

    @QueryMapping("hello")
    public SampleResponse hello() {
        UserResponse user = new UserResponse("1", "Ade Santito", "ade.santito@example.com");
        return new SampleResponse("Ade", "Hello from Adepuu!", user);
    }

    @QueryMapping("simple")
    @PreAuthorize("hasRole('USER')")
    public String simple() {
        return "Hello from simple query! (Authenticated)";
    }

    @QueryMapping("getUser")
    @PreAuthorize("hasRole('MODERATOR') or hasRole('ADMIN')")
    public UserResponse getUser(@Argument("id") String id) {
        // In a real application, you would fetch the user from a database or service.
        if (!userDatabase.containsKey(id)) {
            return null;
        }
        return userDatabase.get(id);
    }

    @MutationMapping("registerUser")
    public UserResponse registerUser(@Argument("input") UserRegisterRequest input) {
        // In a real application, you would save the user to a database.
        UserResponse newUser = new UserResponse(UUID.randomUUID().toString(), input.getName(), input.getEmail());
        userDatabase.put(newUser.getId(), newUser);
        return newUser;
    }
}