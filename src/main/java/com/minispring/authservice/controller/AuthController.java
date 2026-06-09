package com.minispring.authservice.controller;

import com.minispring.authservice.dto.LoginRequestDto;
import com.minispring.authservice.dto.RegisterRequestDto;
import com.minispring.authservice.dto.RegisterResponseDto;
import com.minispring.authservice.dto.TokenResponseDto;
import com.minispring.authservice.dto.TokenValidationResponse;
import com.minispring.authservice.service.KeycloakService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

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
    public ResponseEntity<TokenValidationResponse> validate(@RequestParam String token) {
        return ResponseEntity.ok(keycloakService.validateToken(token));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponseDto> refresh(@RequestParam String refreshToken) {
        TokenResponseDto token = keycloakService.refreshToken(refreshToken);
        return ResponseEntity.ok(token);
    }

    @PutMapping("/{userId}/status")
    public ResponseEntity<Void> changeStatus(@PathVariable UUID userId, @RequestParam boolean enabled){
        keycloakService.changeUserStatus(userId, enabled);
        return ResponseEntity.ok().build();
    }
}
