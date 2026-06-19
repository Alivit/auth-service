package com.minispring.authservice.service.impl;

import com.minispring.authservice.dto.KeycloakIntrospectResponseDto;
import com.minispring.authservice.dto.LoginRequestDto;
import com.minispring.authservice.dto.RegisterRequestDto;
import com.minispring.authservice.dto.RegisterResponseDto;
import com.minispring.authservice.dto.TokenResponseDto;
import com.minispring.authservice.dto.TokenValidationResponseDto;
import com.minispring.authservice.exception.InvalidCredentialsException;
import com.minispring.authservice.exception.ResourceAlreadyExistsException;
import com.minispring.authservice.exception.ResourceNotFoundException;
import com.minispring.authservice.exception.TokenExpiredException;
import com.minispring.authservice.mapper.KeycloakMapper;
import com.minispring.authservice.service.KeycloakService;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
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

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static com.minispring.authservice.exception.ExceptionAnswer.INVALID_CREDENTIALS;
import static com.minispring.authservice.exception.ExceptionAnswer.SESSION_EXPIRED;
import static com.minispring.authservice.exception.ExceptionAnswer.USER_EXIST;
import static com.minispring.authservice.exception.ExceptionAnswer.USER_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakServiceImpl implements KeycloakService {

    private final RestClient restClient;
    private final Keycloak keycloakClient;
    private final KeycloakMapper mapper;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    @Value("${keycloak.realm:demo}")
    private String realm;

    public RegisterResponseDto register(RegisterRequestDto registerRequest) {
        UserRepresentation user = mapper.toUserRepresentation(registerRequest);

        try (Response response = keycloakClient.realm(realm).users().create(user)) {
            if (response.getStatus() == HttpStatus.CREATED.value()) {
                return new RegisterResponseDto(CreatedResponseUtil.getCreatedId(response));
            }
            if (response.getStatus() == HttpStatus.CONFLICT.value()) {
                throw new ResourceAlreadyExistsException(
                        String.format(USER_EXIST, registerRequest.email(), registerRequest.login())
                );
            }
            throw new RuntimeException("Keycloak returned status: " + response.getStatus());
        }
    }

    public TokenResponseDto authenticate(LoginRequestDto loginRequest) {
        MultiValueMap<String, String> keycloakRequest =
                mapper.dtoToLoginRequest(loginRequest, clientId, clientSecret);
        try {
            return restClient.post()
                    .uri("/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(keycloakRequest)
                    .retrieve()
                    .body(TokenResponseDto.class);

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
            throw new RuntimeException("Keycloak API failure: HTTP " + e.getResponse().getStatus());
        }
    }

    public TokenResponseDto refreshToken(String refreshToken) {
        MultiValueMap<String, String> keycloakRequest =
                mapper.dtoToRefreshTokenRequest(refreshToken, clientId, clientSecret);

        try {
            return restClient.post()
                    .uri("/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(keycloakRequest)
                    .retrieve()
                    .body(TokenResponseDto.class);

        } catch (HttpClientErrorException.BadRequest ex) {
            Map<String, String> error = ex.getResponseBodyAs(new ParameterizedTypeReference<>() {
            });
            if (error != null && "invalid_grant".equals(error.get("error"))) {
                throw new TokenExpiredException(SESSION_EXPIRED);
            }
            throw ex;
        }
    }

    public TokenValidationResponseDto validateToken(String token) {
        MultiValueMap<String, String> keycloakRequest = mapper.dtoToTokenRequest(token, clientId, clientSecret);

        try {
            KeycloakIntrospectResponseDto responseDto = restClient.post()
                    .uri("/token/introspect")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(keycloakRequest)
                    .retrieve()
                    .body(KeycloakIntrospectResponseDto.class);

            if (responseDto == null || !responseDto.active()) {
                return new TokenValidationResponseDto(null, null, null, false, Collections.emptySet());
            }

            return mapper.toTokenValidationResponse(responseDto);

        } catch (Exception ex) {
            log.error("Failed to introspect token in Keycloak", ex);
            return new TokenValidationResponseDto(null, null, null, false, Collections.emptySet());
        }
    }
}
