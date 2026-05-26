package com.minispring.authservice.dto;

public record TokenResponseDto(
        String accessToken,
        String refreshToken
) {
}
