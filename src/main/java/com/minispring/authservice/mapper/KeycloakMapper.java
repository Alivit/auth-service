package com.minispring.authservice.mapper;

import com.minispring.authservice.dto.request.LoginRequest;
import com.minispring.authservice.dto.request.RegisterRequest;
import com.minispring.authservice.dto.response.KeycloakIntrospectResponse;
import com.minispring.authservice.dto.response.TokenValidationResponse;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING,
        imports = {List.class},
        builder = @Builder(disableBuilder = true))
public interface KeycloakMapper {

    @Mapping(target = "username", source = "login")
    @Mapping(target = "enabled", constant = "true")
    @Mapping(target = "emailVerified", constant = "false")
    @Mapping(target = "requiredActions", expression = "java(java.util.List.of())")
    @Mapping(target = "credentials", expression = "java(List.of(toCredential(registerRequest.password())))")
    UserRepresentation toUserRepresentation(RegisterRequest registerRequest);

    @Mapping(target = "type", constant = CredentialRepresentation.PASSWORD)
    @Mapping(target = "temporary", constant = "false")
    @Mapping(target = "value", source = "password")
    CredentialRepresentation toCredential(String password);

    @Mapping(target = "username", source = "preferredUsername")
    @Mapping(target = "userId", source = "sub")
    @Mapping(target = "roles", source = "realmAccess.roles", qualifiedByName = "filterKeycloakRoles")
    TokenValidationResponse toTokenValidationResponse(KeycloakIntrospectResponse response);

    @Named("filterKeycloakRoles")
    default Set<String> filterKeycloakRoles(List<String> roles) {
        if (roles == null) return Set.of();

        return roles.stream().filter(role -> role.startsWith("ROLE_")).collect(Collectors.toUnmodifiableSet());
    }

    default MultiValueMap<String, String> dtoToLoginRequest(LoginRequest dto, String clientId) {
        LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", clientId);
        map.add("grant_type", "password");
        map.add("username", dto.login());
        map.add("password", dto.password());

        if (StringUtils.hasText(dto.otpCode())) {
            map.add("totp", dto.otpCode().trim());
        }
        return map;
    }

    default MultiValueMap<String, String> dtoToRefreshTokenRequest(String refreshToken, String clientId) {
        LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", clientId);
        map.add("grant_type", "refresh_token");
        map.add("refresh_token", refreshToken);
        return map;
    }

    default MultiValueMap<String, String> dtoToTokenRequest(String token, String clientId) {
        LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", clientId);
        map.add("token", token);
        return map;
    }
}
