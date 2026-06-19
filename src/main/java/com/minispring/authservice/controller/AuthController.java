package com.minispring.authservice.controller;

import com.minispring.authservice.dto.LoginRequestDto;
import com.minispring.authservice.dto.RegisterRequestDto;
import com.minispring.authservice.dto.RegisterResponseDto;
import com.minispring.authservice.dto.TokenRequestDto;
import com.minispring.authservice.dto.TokenResponseDto;
import com.minispring.authservice.dto.TokenValidationResponseDto;
import com.minispring.authservice.service.KeycloakService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final KeycloakService keycloakService;

    @PostMapping("/registration")
    public ResponseEntity<RegisterResponseDto> createCredentials(@RequestBody  @Valid RegisterRequestDto registerRequest) {
        RegisterResponseDto response = keycloakService.register(registerRequest);
        URI location = URI.create("/api/v1/users/" + response.userId());
        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponseDto> login(@RequestBody  @Valid LoginRequestDto loginRequest) {
        TokenResponseDto token = keycloakService.authenticate(loginRequest);
        return ResponseEntity.ok(token);
    }

    @PostMapping("/validate")
    public ResponseEntity<TokenValidationResponseDto> validate(@Valid @RequestBody TokenRequestDto request) {
        return ResponseEntity.ok(keycloakService.validateToken(request.token()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponseDto> refresh(@Valid @RequestBody TokenRequestDto request) {
        TokenResponseDto token = keycloakService.refreshToken(request.token());
        return ResponseEntity.ok(token);
    }

}
