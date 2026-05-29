package com.minispring.authservice.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;

public record TokenResponse(
        @JsonAlias("access_token") String accessToken,

        @JsonAlias("refresh_token") String refreshToken) {}
