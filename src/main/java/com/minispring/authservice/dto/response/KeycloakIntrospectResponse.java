package com.minispring.authservice.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;

public record KeycloakIntrospectResponse(
        String sub,
        @JsonAlias("preferred_username") String preferredUsername,
        String email,
        boolean active,
        @JsonAlias("realm_access") RealmAccess realmAccess) {
    public record RealmAccess(List<String> roles) {}
}
