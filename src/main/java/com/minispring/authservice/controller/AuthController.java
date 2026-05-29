package com.minispring.authservice.controller;

import com.minispring.authservice.dto.request.LoginRequest;
import com.minispring.authservice.dto.request.RegisterRequest;
import com.minispring.authservice.dto.request.TokenRequest;
import com.minispring.authservice.dto.response.RegisterResponse;
import com.minispring.authservice.dto.response.TokenResponse;
import com.minispring.authservice.dto.response.TokenValidationResponse;
import com.minispring.authservice.service.KeycloakService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final KeycloakService keycloakService;

    @PostMapping("/registration")
    public ResponseEntity<RegisterResponse> createCredentials(@RequestBody @Valid RegisterRequest registerRequest) {
        RegisterResponse response = keycloakService.register(registerRequest);
        URI location = URI.create("api/v1/users/" + response.userId());
        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid LoginRequest loginRequest) {
        TokenResponse token = keycloakService.authenticate(loginRequest);
        return ResponseEntity.ok(token);
    }

    @PostMapping("/validate")
    public ResponseEntity<TokenValidationResponse> validate(@Valid @RequestBody TokenRequest request) {
        return ResponseEntity.ok(keycloakService.validateToken(request.token()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody TokenRequest request) {
        TokenResponse token = keycloakService.refreshToken(request.token());
        return ResponseEntity.ok(token);
    }

    @DeleteMapping("/rollback/username/{username}")
    public ResponseEntity<Void> rollbackRegistrationByUsername(@PathVariable String username) {
        keycloakService.deleteUserByUsername(username);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/rollback/id/{userId}")
    public ResponseEntity<Void> rollbackRegistration(@PathVariable UUID userId) {
        keycloakService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
