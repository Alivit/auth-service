package com.minispring.authservice.mapper;

import com.minispring.authservice.dto.LoginRequestDto;
import com.minispring.authservice.dto.RegisterRequestDto;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING,
        imports = {List.class},
        builder = @Builder(disableBuilder = true))
public interface KeycloakUserMapper {

    @Mapping(target = "username", source = "login")
    @Mapping(target = "enabled", constant = "true")
    @Mapping(target = "emailVerified", constant = "true")
    @Mapping(target = "requiredActions", expression = "java(java.util.List.of())")
    @Mapping(target = "credentials", expression = "java(List.of(toCredential(registerRequest.password())))")
    UserRepresentation toUserRepresentation(RegisterRequestDto registerRequest);

    @Mapping(target = "type", constant = CredentialRepresentation.PASSWORD)
    @Mapping(target = "temporary", constant = "false")
    @Mapping(target = "value", source = "password")
    CredentialRepresentation toCredential(String password);

    default MultiValueMap<String, String> dtoToLoginRequest(
            LoginRequestDto dto, String clientId, String clientSecret) {
        MultiValueMap<String, String> map = initRequest(clientId, clientSecret);
        map.add("grant_type", "password");
        map.add("username", dto.login());
        map.add("password", dto.password());

        return map;
    }

    default MultiValueMap<String, String> dtoToRefreshTokenRequest(
            String refreshToken, String clientId, String clientSecret) {
        MultiValueMap<String, String> map = initRequest(clientId, clientSecret);
        map.add("grant_type", "refresh_token");
        map.add("refresh_token", refreshToken);

        return map;
    }

    default MultiValueMap<String, String> dtoToTokenRequest(
            String token, String clientId, String clientSecret) {
        MultiValueMap<String, String> map = initRequest(clientId, clientSecret);
        map.add("token", token);

        return map;
    }

    default MultiValueMap<String, String> initRequest(String clientId, String clientSecret) {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);
        return map;
    }
}
