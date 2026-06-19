package com.minispring.authservice.dto;

import java.util.Set;

public record TokenValidationResponseDto(
        String userId,
        String username,
        String email,
        boolean active,
        Set<String> roles
) {
}
