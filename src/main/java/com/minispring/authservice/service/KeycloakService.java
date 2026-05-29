package com.minispring.authservice.service;

import com.minispring.authservice.dto.LoginRequestDto;
import com.minispring.authservice.dto.RegisterRequestDto;
import com.minispring.authservice.dto.RegisterResponseDto;
import com.minispring.authservice.dto.TokenResponseDto;
import com.minispring.authservice.dto.TokenValidationResponseDto;

import java.util.UUID;

public interface KeycloakService {
    RegisterResponseDto register(RegisterRequestDto registerRequest);

    TokenResponseDto authenticate(LoginRequestDto loginRequestDto);

    TokenResponseDto refreshToken(String refreshToken);

    TokenValidationResponseDto validateToken(String token);

    void changeUserStatus(UUID userId, boolean enabled);
}
