package com.minispring.authservice.service.impl;

import com.minispring.authservice.dto.LoginRequestDto;
import com.minispring.authservice.dto.RegisterRequestDto;
import com.minispring.authservice.dto.RegisterResponseDto;
import com.minispring.authservice.dto.TokenResponseDto;
import com.minispring.authservice.dto.TokenValidationResponse;
import com.minispring.authservice.mapper.KeycloakUserMapper;
import com.minispring.authservice.service.KeycloakService;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class KeycloakServiceImpl implements KeycloakService {

    private final RestClient restClient;
    private final Keycloak keycloakClient;
    private final KeycloakUserMapper mapper;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    @Value("${keycloak.realm:demo}")
    private String realm;

    public RegisterResponseDto register(RegisterRequestDto registerRequest) {
        UserRepresentation user = mapper.toUserRepresentation(registerRequest);

        try (Response response = keycloakClient.realm(realm).users().create(user)) {
            if (response.getStatus() == 201) {
                return new RegisterResponseDto(CreatedResponseUtil.getCreatedId(response));
            }
            throw new RuntimeException("Keycloak returned status: " + response.getStatus());
        }
    }

    public TokenResponseDto authenticate(LoginRequestDto loginRequestDto) {
        MultiValueMap<String, String> keycloakRequest = initRequest();
        keycloakRequest.add("grant_type", "password");
        keycloakRequest.add("username", loginRequestDto.login());
        keycloakRequest.add("password", loginRequestDto.password());

        return restClient.post()
                .uri("/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(keycloakRequest)
                .retrieve()
                .body(TokenResponseDto.class);
    }

    public TokenResponseDto refreshToken(String refreshToken) {
        MultiValueMap<String, String> formData = initRequest();
        formData.add("grant_type", "refresh_token");
        formData.add("refresh_token", refreshToken);

        return restClient.post()
                .uri("/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .body(TokenResponseDto.class);
    }

    public TokenValidationResponse validateToken(String token) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("token", token);
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);

        return restClient.post()
                .uri("/token/introspect")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .body(TokenValidationResponse.class);
    }

    private MultiValueMap<String, String> initRequest(){
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);
        return map;
    }
}
