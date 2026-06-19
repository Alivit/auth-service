package com.minispring.authservice.controller;

import com.minispring.authservice.BaseIntegrationTest;
import com.minispring.authservice.dto.LoginRequestDto;
import com.minispring.authservice.dto.RegisterRequestDto;
import com.minispring.authservice.dto.TokenRequestDto;
import com.minispring.authservice.dto.TokenResponseDto;
import com.minispring.authservice.dto.TokenValidationResponseDto;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.json.JsonMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.field;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@AutoConfigureMockMvc
class AuthControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvcTester mockMvcTester;

    @Autowired
    private JsonMapper jsonMapper;

    private static final String BASE_URL = "/api/v1/auth";
    private static final String TEST_OTP_SECRET = "GEZDGNBVGY3TQOJQ";

    private RegisterRequestDto existingUserRequest;

    @BeforeEach
    public void setUpExistingUser() {
        existingUserRequest = Instancio.of(RegisterRequestDto.class)
                .generate(field(RegisterRequestDto::email), gen -> gen.text().pattern("#c#c#c#c#c#c@testdomain.com"))
                .generate(field(RegisterRequestDto::login), gen -> gen.text().pattern("user_#c#c#c#c#c"))
                .set(field(RegisterRequestDto::password), "TestPassword123.")
                .create();

        mockMvcTester.perform(post(BASE_URL + "/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(existingUserRequest)));
    }

    @Nested
    class RegistrationTest {

        @Test
        void registrationShouldReturnCreatedStatusAndLocationHeader() {
            RegisterRequestDto newRequest = Instancio.of(RegisterRequestDto.class)
                    .generate(field(RegisterRequestDto::email), gen -> gen.text().pattern("new_#c#c#c#c@domain.com"))
                    .generate(field(RegisterRequestDto::login), gen -> gen.text().pattern("new_user_#c#c#c"))
                    .set(field(RegisterRequestDto::password), "Password123.")
                    .create();

            MvcTestResult result = mockMvcTester.perform(post(BASE_URL + "/registration")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(newRequest)));

            assertThat(result).hasStatus(HttpStatus.CREATED);

            assertThat(result).headers()
                    .extracting(h -> h.getFirst("Location"))
                    .asString()
                    .contains("/api/v1/users/");

            assertThat(result).bodyJson()
                    .hasPath("$.userId");
        }

        @Test
        void registrationShouldReturnConflictWhenUserAlreadyExists() {
            RegisterRequestDto duplicateRequest = Instancio.of(RegisterRequestDto.class)
                    .set(field(RegisterRequestDto::email), existingUserRequest.email())
                    .set(field(RegisterRequestDto::login), existingUserRequest.login())
                    .set(field(RegisterRequestDto::password), "NewPassword123.")
                    .create();

            MvcTestResult result = mockMvcTester.perform(post(BASE_URL + "/registration")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(duplicateRequest)));

            assertThat(result).hasStatus(HttpStatus.CONFLICT);
        }
    }

    @Nested
    class LoginTest {

        @Test
        void loginShouldReturnTokensWhenCredentialsAreValid() {
            LoginRequestDto loginRequest = new LoginRequestDto(
                    existingUserRequest.login(),
                    existingUserRequest.password(),
                    null
            );

            MvcTestResult result = mockMvcTester.perform(post(BASE_URL + "/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(loginRequest)));

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .hasPath("$.access_token")
                    .hasPath("$.refresh_token");
        }

        @Test
        void loginAdminWithoutOtpShouldReturnBadRequest() {
            LoginRequestDto loginRequest = new LoginRequestDto(
                    "test-otp-admin",
                    "TestPassword123.",
                    null
            );

            MvcTestResult result = mockMvcTester.perform(post(BASE_URL + "/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(loginRequest)));

            assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        }

        @Test
        void loginAdminWithValidOtpShouldReturnTokens() {
            String dynamicCode = generateCurrentTOTP(TEST_OTP_SECRET);

            LoginRequestDto loginRequest = new LoginRequestDto(
                    "TestAdmin",
                    "TestPassword123.",
                    dynamicCode
            );

            MvcTestResult result = mockMvcTester.post().uri(BASE_URL + "/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(loginRequest))
                    .exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson()
                    .hasPath("$.access_token")
                    .hasPath("$.refresh_token");
        }

        @Test
        void loginShouldReturnUnauthorizedWhenPasswordIsWrong() {
            LoginRequestDto badRequest = new LoginRequestDto(existingUserRequest.login(), "InvalidPassword", null);

            MvcTestResult result = mockMvcTester.perform(post(BASE_URL + "/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(badRequest)));

            assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        }

        @Test
        void loginShouldReturnUnauthorizedWhenUserDoesNotExist() {
            LoginRequestDto nonExistentUserRequest = Instancio.of(LoginRequestDto.class)
                    .set(field(LoginRequestDto::password), "NewPassword123.")
                    .set(field(LoginRequestDto::otpCode), null)
                    .create();

            MvcTestResult result = mockMvcTester.perform(post(BASE_URL + "/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(nonExistentUserRequest)));

            assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    class TokenValidationTest {

        @Test
        void validateShouldReturnTrueForValidToken() throws Exception {
            LoginRequestDto loginRequest = new LoginRequestDto(existingUserRequest.login(), existingUserRequest.password(), null);
            MvcTestResult loginResult = mockMvcTester.perform(post(BASE_URL + "/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(loginRequest)));

            String responseContent = loginResult.getResponse().getContentAsString();
            TokenResponseDto tokens = jsonMapper.readValue(responseContent, TokenResponseDto.class);

            TokenRequestDto validationRequest = new TokenRequestDto(tokens.accessToken());

            MvcTestResult result = mockMvcTester.perform(post(BASE_URL + "/validate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(validationRequest)));

            assertThat(result).hasStatusOk();

            TokenValidationResponseDto expectedResponse = new TokenValidationResponseDto(
                    null,
                    existingUserRequest.login(),
                    existingUserRequest.email(),
                    true,
                    java.util.Set.of("ROLE_USER")
            );

            assertThat(result).bodyJson()
                    .convertTo(TokenValidationResponseDto.class)
                    .usingRecursiveComparison()
                    .ignoringFields("userId")
                    .isEqualTo(expectedResponse);
        }

        @Test
        void validateShouldReturnTrueForValidAdminToken() throws Exception {
            String dynamicCode = generateCurrentTOTP(TEST_OTP_SECRET);

            LoginRequestDto loginRequest = new LoginRequestDto(
                    "TestAdminTwo",
                    "TestPassword123.",
                    dynamicCode
            );

            MvcTestResult loginResult = mockMvcTester.post().uri(BASE_URL + "/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(loginRequest))
                    .exchange();

            String responseContent = loginResult.getResponse().getContentAsString();
            TokenResponseDto tokens = jsonMapper.readValue(responseContent, TokenResponseDto.class);

            TokenRequestDto validationRequest = new TokenRequestDto(tokens.accessToken());

            MvcTestResult result = mockMvcTester.post().uri(BASE_URL + "/validate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(validationRequest))
                    .exchange();

            assertThat(result).hasStatusOk();

            TokenValidationResponseDto expectedResponse = new TokenValidationResponseDto(
                    null,
                    "testadmintwo",
                    "testadmintwo@company.com",
                    true,
                    java.util.Set.of("ROLE_ADMIN")
            );

            assertThat(result).bodyJson()
                    .convertTo(TokenValidationResponseDto.class)
                    .usingRecursiveComparison()
                    .ignoringFields("userId")
                    .isEqualTo(expectedResponse);
        }

        @Test
        void validateShouldReturnActiveFalseForInvalidToken() {
            TokenRequestDto requestDto = new TokenRequestDto("eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwi.invalid.token");

            MvcTestResult result = mockMvcTester.perform(post(BASE_URL + "/validate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(requestDto)));

            assertThat(result).hasStatusOk();

            TokenValidationResponseDto expectedResponse = new TokenValidationResponseDto(
                    null, null, null, false, java.util.Collections.emptySet()
            );

            assertThat(result).bodyJson()
                    .convertTo(TokenValidationResponseDto.class)
                    .usingRecursiveComparison()
                    .isEqualTo(expectedResponse);
        }
    }

    @Nested
    class TokenRefreshTest {

        @Test
        void refreshShouldReturnNewTokensWhenRefreshTokenIsValid() throws Exception {
            LoginRequestDto loginRequest = new LoginRequestDto(existingUserRequest.login(), existingUserRequest.password(), null);
            MvcTestResult loginResult = mockMvcTester.perform(post(BASE_URL + "/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(loginRequest)));

            String responseContent = loginResult.getResponse().getContentAsString();
            TokenResponseDto tokens = jsonMapper.readValue(responseContent, TokenResponseDto.class);

            TokenRequestDto refreshRequest = new TokenRequestDto(tokens.refreshToken());

            MvcTestResult result = mockMvcTester.perform(post(BASE_URL + "/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(refreshRequest)));

            assertThat(result).hasStatusOk();

            assertThat(result).bodyJson()
                    .convertTo(TokenResponseDto.class)
                    .usingRecursiveComparison()
                    .comparingOnlyFields("accessToken", "refreshToken")
                    .isNotNull();

            assertThat(result).bodyJson()
                    .hasPath("$.access_token")
                    .hasPath("$.refresh_token");
        }

        @Test
        void refreshShouldReturnUnauthorizedWhenRefreshTokenIsInvalid() {
            TokenRequestDto requestDto = new TokenRequestDto("invalidToken");

            MvcTestResult result = mockMvcTester.perform(post(BASE_URL + "/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(requestDto)));

            assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }

    private String generateCurrentTOTP(String secretBase32) {
        try {
            byte[] bytes = secretBase32.getBytes(StandardCharsets.UTF_8);

            long timeIndex = (System.currentTimeMillis() / 1000) / 30;
            byte[] payload = ByteBuffer.allocate(8).putLong(timeIndex).array();

            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(bytes, "HmacSHA1"));
            byte[] hash = mac.doFinal(payload);

            int offset = hash[hash.length - 1] & 0xf;
            int binary = ((hash[offset] & 0x7f) << 24) |
                    ((hash[offset + 1] & 0xff) << 16) |
                    ((hash[offset + 2] & 0xff) << 8) |
                    (hash[offset + 3] & 0xff);

            int otp = binary % 1000000;
            return String.format("%06d", otp);
        } catch (Exception e) {
            throw new RuntimeException("Failed to automatically generate test TOTP", e);
        }
    }
}