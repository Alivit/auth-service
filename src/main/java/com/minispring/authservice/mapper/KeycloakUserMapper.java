package com.minispring.authservice.mapper;

import com.minispring.authservice.dto.RegisterRequestDto;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING,
        imports = {List.class},
        builder = @Builder(disableBuilder = true))
public interface KeycloakUserMapper {

    @Mapping(target = "enabled", constant = "true")
    @Mapping(target = "credentials", expression = "java(List.of(toCredential(registerRequest.password())))")
    UserRepresentation toUserRepresentation(RegisterRequestDto registerRequest);

    @Mapping(target = "type", constant = CredentialRepresentation.PASSWORD)
    @Mapping(target = "temporary", constant = "false")
    @Mapping(target = "value", source = "password")
    CredentialRepresentation toCredential(String password);
}
