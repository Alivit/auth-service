package com.minispring.authservice.service.impl;

import static com.minispring.authservice.exception.ExceptionAnswer.INVALID_CREDENTIALS;
import static com.minispring.authservice.exception.ExceptionAnswer.SESSION_EXPIRED;
import static com.minispring.authservice.exception.ExceptionAnswer.USER_EXIST;
import static com.minispring.authservice.exception.ExceptionAnswer.USER_NOT_FOUND;

import com.minispring.authservice.dto.request.LoginRequest;
import com.minispring.authservice.dto.request.RegisterRequest;
import com.minispring.authservice.dto.response.KeycloakIntrospectResponse;
import com.minispring.authservice.dto.response.RegisterResponse;
import com.minispring.authservice.dto.response.TokenResponse;
import com.minispring.authservice.dto.response.TokenValidationResponse;
import com.minispring.authservice.exception.InvalidCredentialsException;
import com.minispring.authservice.exception.ResourceAlreadyExistsException;
import com.minispring.authservice.exception.ResourceNotFoundException;
import com.minispring.authservice.exception.TokenExpiredException;
import com.minispring.authservice.mapper.KeycloakMapper;
import com.minispring.authservice.service.KeycloakService;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakServiceImpl implements KeycloakService {

    private final RestClient restClient;
    private final Keycloak keycloakClient;
    private final KeycloakMapper mapper;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.realm:demo}")
    private String realm;

    public RegisterResponse register(RegisterRequest registerRequest) {
        UserRepresentation user = mapper.toUserRepresentation(registerRequest);

        try (Response response = keycloakClient.realm(realm).users().create(user)) {
            if (response.getStatus() == HttpStatus.CREATED.value()) {
                return new RegisterResponse(CreatedResponseUtil.getCreatedId(response));
            }
            if (response.getStatus() == HttpStatus.CONFLICT.value()) {
                throw new ResourceAlreadyExistsException(
                        String.format(USER_EXIST, registerRequest.email(), registerRequest.login()));
            }
            throw new RuntimeException("Keycloak returned status: " + response.getStatus());
        }
    }

    public TokenResponse authenticate(LoginRequest loginRequest) {
        MultiValueMap<String, String> keycloakRequest = mapper.dtoToLoginRequest(loginRequest, clientId);
        try {
            return restClient
                    .post()
                    .uri("/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(keycloakRequest)
                    .retrieve()
                    .body(TokenResponse.class);

        } catch (HttpClientErrorException.BadRequest ex) {
            Map<String, String> error = ex.getResponseBodyAs(new ParameterizedTypeReference<>() {});
            if (error != null && "invalid_grant".equals(error.get("error"))) {
                throw new InvalidCredentialsException(INVALID_CREDENTIALS);
            }
            throw ex;
        }
    }

    public void changeUserStatus(UUID userId, boolean enabled) {
        try {
            UserResource userResource = keycloakClient.realm(realm).users().get(userId.toString());
            UserRepresentation userRepresentation = userResource.toRepresentation();

            userRepresentation.setEnabled(enabled);
            userResource.update(userRepresentation);

            if (!enabled) {
                userResource.logout();
            }
        } catch (NotFoundException e) {
            throw new ResourceNotFoundException(String.format(USER_NOT_FOUND, userId));
        } catch (WebApplicationException e) {
            if (e.getResponse().getStatus() == 404) {
                throw new ResourceNotFoundException(String.format(USER_NOT_FOUND, userId));
            }
            throw new RuntimeException(
                    "Keycloak API failure: HTTP " + e.getResponse().getStatus());
        }
    }

    public TokenResponse refreshToken(String refreshToken) {
        MultiValueMap<String, String> keycloakRequest = mapper.dtoToRefreshTokenRequest(refreshToken, clientId);

        try {
            return restClient
                    .post()
                    .uri("/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(keycloakRequest)
                    .retrieve()
                    .body(TokenResponse.class);

        } catch (HttpClientErrorException.BadRequest ex) {
            Map<String, String> error = ex.getResponseBodyAs(new ParameterizedTypeReference<>() {});
            if (error != null && "invalid_grant".equals(error.get("error"))) {
                throw new TokenExpiredException(SESSION_EXPIRED);
            }
            throw ex;
        }
    }

    public TokenValidationResponse validateToken(String token) {
        MultiValueMap<String, String> keycloakRequest = mapper.dtoToTokenRequest(token, clientId);

        try {
            KeycloakIntrospectResponse responseDto = restClient
                    .post()
                    .uri("/token/introspect")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(keycloakRequest)
                    .retrieve()
                    .body(KeycloakIntrospectResponse.class);

            if (responseDto == null || !responseDto.active()) {
                return new TokenValidationResponse(null, null, null, false, Collections.emptySet());
            }

            return mapper.toTokenValidationResponse(responseDto);

        } catch (Exception ex) {
            log.error("Failed to introspect token in Keycloak", ex);
            return new TokenValidationResponse(null, null, null, false, Collections.emptySet());
        }
    }

    @Retry(name = "keycloakDelete", fallbackMethod = "recoverFailedDeletion")
    public void deleteUser(UUID userId) {
        try {
            log.warn("Compensating transaction: Removing user {} from Keycloak", userId);
            keycloakClient.realm(realm).users().get(userId.toString()).remove();
        } catch (jakarta.ws.rs.NotFoundException e) {
            log.info("User {} already deleted or not found in Keycloak", userId);
        }
    }

    @Override
    @Retry(name = "keycloakDelete", fallbackMethod = "recoverFailedDeletionByUsername")
    public void deleteUserByUsername(String username) {
        log.warn("Compensating transaction: Trying to remove user {} by username from Keycloak", username);

        List<UserRepresentation> users = keycloakClient.realm(realm).users().search(username, true);

        if (users != null && !users.isEmpty()) {
            String userId = users.getFirst().getId();
            keycloakClient.realm(realm).users().get(userId).remove();
            log.info("Successfully rolled back user {} (id: {}) from Keycloak", username, userId);
        } else {
            log.info("User {} not found in Keycloak during rollback. Nothing to delete.", username);
        }
    }

    public void recoverFailedDeletionByUsername(String username, Throwable t) {
        log.error("!!! CRITICAL: Keycloak hard-delete failed for username: {}. Reason: {}", username, t.getMessage());
    }

    public void recoverFailedDeletion(UUID userId, Throwable t) {
        log.error(
                "!!! CRITICAL: Keycloak hard-delete completely failed after 4 attempts for user: {}. Reason: {}",
                userId,
                t.getMessage());
    }
}
