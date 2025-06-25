package com.adepuu.blog.delivery.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SampleResponse {
    private String name;
    private String message;
    private UserResponse user;
}
