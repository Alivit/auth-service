package com.minispring.authservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TokenRequest(
        @NotBlank(message = "Token cannot be empty") String token) {
    public TokenRequest {
        if (token != null) {
            token = token.trim();
        }
    }
}
