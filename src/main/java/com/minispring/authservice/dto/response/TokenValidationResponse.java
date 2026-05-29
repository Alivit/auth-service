package com.minispring.authservice.dto.response;

import java.util.Set;

public record TokenValidationResponse(String userId, String username, String email, boolean active, Set<String> roles) {
    public TokenValidationResponse {
        roles = (roles != null) ? Set.copyOf(roles) : Set.of();
    }
}
