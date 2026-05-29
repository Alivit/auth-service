package com.minispring.authservice.service;

import com.minispring.authservice.dto.request.LoginRequest;
import com.minispring.authservice.dto.request.RegisterRequest;
import com.minispring.authservice.dto.response.RegisterResponse;
import com.minispring.authservice.dto.response.TokenResponse;
import com.minispring.authservice.dto.response.TokenValidationResponse;
import java.util.UUID;

public interface KeycloakService {
    RegisterResponse register(RegisterRequest registerRequest);

    TokenResponse authenticate(LoginRequest loginRequest);

    TokenResponse refreshToken(String refreshToken);

    TokenValidationResponse validateToken(String token);

    void changeUserStatus(UUID userId, boolean enabled);

    void deleteUser(UUID userId);

    void deleteUserByUsername(String username);
}
