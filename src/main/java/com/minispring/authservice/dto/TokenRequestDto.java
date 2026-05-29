package com.minispring.authservice.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenRequestDto(
        @NotBlank(message = "Token cannot be empty")
        String token
) {
}
