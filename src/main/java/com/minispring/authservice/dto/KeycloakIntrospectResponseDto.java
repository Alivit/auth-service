package com.minispring.authservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record KeycloakIntrospectResponseDto(
        String sub,
        @JsonProperty("preferred_username")
        String preferredUsername,
        String email,
        boolean active,
        @JsonProperty("realm_access")
        RealmAccess realmAccess
) {
    public record RealmAccess(List<String> roles) {}
}
