package com.minispring.authservice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.field;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import com.minispring.authservice.BaseIntegrationTest;
import com.minispring.authservice.dto.request.LoginRequest;
import com.minispring.authservice.dto.request.RegisterRequest;
import com.minispring.authservice.dto.request.TokenRequest;
import com.minispring.authservice.dto.response.TokenResponse;
import com.minispring.authservice.dto.response.TokenValidationResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.json.JsonMapper;

@AutoConfigureMockMvc
class AuthControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvcTester mockMvcTester;

    @Autowired
    private JsonMapper jsonMapper;

    private static final String BASE_URL = "/api/v1/auth";
    private static final String TEST_OTP_SECRET = "GEZDGNBVGY3TQOJQ";
    private static final String ADMIN_USERNAME = "TestAdmin";
    private static final String ADMIN_PASSWORD = "TestPassword123.";

    private RegisterRequest existingUserRequest;

    @BeforeEach
    public void setUpExistingUser() {
        existingUserRequest = Instancio.of(RegisterRequest.class)
                .generate(field(RegisterRequest::email), gen -> gen.text().pattern("#c#c#c#c#c#c@testdomain.com"))
                .generate(field(RegisterRequest::login), gen -> gen.text().pattern("user_#c#c#c#c#c"))
                .set(field(RegisterRequest::password), "TestPassword123.")
                .create();

        performPost("/registration", existingUserRequest);
    }

    @Nested
    class RegistrationTest {

        @Test
        void registrationShouldReturnCreatedStatusAndLocationHeader() {
            RegisterRequest newRequest = Instancio.of(RegisterRequest.class)
                    .generate(field(RegisterRequest::email), gen -> gen.text().pattern("new_#c#c#c#c@domain.com"))
                    .generate(field(RegisterRequest::login), gen -> gen.text().pattern("new_user_#c#c#c"))
                    .set(field(RegisterRequest::password), "Password123.")
                    .create();

            MvcTestResult result = performPost("/registration", newRequest);

            assertThat(result).hasStatus(HttpStatus.CREATED);
            assertThat(result)
                    .headers()
                    .extracting(h -> h.getFirst("Location"))
                    .asString()
                    .contains("api/v1/users/");
            assertThat(result).bodyJson().hasPath("$.userId");
        }

        @Test
        void registrationShouldReturnConflictWhenUserAlreadyExists() {
            RegisterRequest duplicateRequest = Instancio.of(RegisterRequest.class)
                    .set(field(RegisterRequest::email), existingUserRequest.email())
                    .set(field(RegisterRequest::login), existingUserRequest.login())
                    .set(field(RegisterRequest::password), "NewPassword123.")
                    .create();

            MvcTestResult result = performPost("/registration", duplicateRequest);

            assertThat(result).hasStatus(HttpStatus.CONFLICT);
        }
    }

    @Nested
    class LoginTest {

        @Test
        void loginShouldReturnTokensWhenCredentialsAreValid() {
            LoginRequest loginRequest =
                    new LoginRequest(existingUserRequest.login(), existingUserRequest.password(), null);
            MvcTestResult result = performPost("/login", loginRequest);

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson().hasPath("$.accessToken").hasPath("$.refreshToken");
        }

        @Test
        void loginAdminWithoutOtpShouldReturnUnauthorized() {
            var loginRequest = new LoginRequest(ADMIN_USERNAME, ADMIN_PASSWORD, null);
            var result = performPost("/login", loginRequest);

            assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
        }

        @Test
        void loginAdminWithValidOtpShouldReturnTokens() {
            LoginRequest loginRequest =
                    new LoginRequest(ADMIN_USERNAME, ADMIN_PASSWORD, generateCurrentTOTP(TEST_OTP_SECRET));
            MvcTestResult result = performPost("/login", loginRequest);

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson().hasPath("$.accessToken").hasPath("$.refreshToken");
        }

        @Test
        void loginShouldReturnBadRequestWhenPasswordIsWrong() {
            LoginRequest badRequest = new LoginRequest(existingUserRequest.login(), "InvalidPassword", null);
            MvcTestResult result = performPost("/login", badRequest);

            assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        }

        @Test
        void loginShouldReturnUnauthorizedWhenUserDoesNotExist() {
            LoginRequest nonExistentUserRequest = Instancio.of(LoginRequest.class)
                    .set(field(LoginRequest::password), "NewPassword123.")
                    .set(field(LoginRequest::otpCode), null)
                    .create();

            MvcTestResult result = performPost("/login", nonExistentUserRequest);

            assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    class TokenValidationTest {

        @Test
        void validateShouldReturnTrueForValidToken() {
            TokenResponse tokens = getValidTokens(existingUserRequest.login(), existingUserRequest.password(), null);
            MvcTestResult result = performPost("/validate", new TokenRequest(tokens.accessToken()));

            assertThat(result).hasStatusOk();

            TokenValidationResponse expectedResponse = new TokenValidationResponse(
                    null, existingUserRequest.login(), existingUserRequest.email(), true, Set.of("ROLE_USER"));

            assertThat(result)
                    .bodyJson()
                    .convertTo(TokenValidationResponse.class)
                    .usingRecursiveComparison()
                    .ignoringFields("userId")
                    .isEqualTo(expectedResponse);
        }

        @Test
        void validateShouldReturnTrueForValidAdminToken() {
            TokenResponse tokens = getValidTokens("TestAdminTwo", ADMIN_PASSWORD, generateCurrentTOTP(TEST_OTP_SECRET));
            MvcTestResult result = performPost("/validate", new TokenRequest(tokens.accessToken()));

            assertThat(result).hasStatusOk();

            TokenValidationResponse expectedResponse = new TokenValidationResponse(
                    null, "testadmintwo", "testadmintwo@company.com", true, Set.of("ROLE_ADMIN"));

            assertThat(result)
                    .bodyJson()
                    .convertTo(TokenValidationResponse.class)
                    .usingRecursiveComparison()
                    .ignoringFields("userId")
                    .isEqualTo(expectedResponse);
        }

        @Test
        void validateShouldReturnActiveFalseForInvalidToken() {
            TokenRequest requestDto = new TokenRequest("eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwi.invalid.token");
            MvcTestResult result = performPost("/validate", requestDto);

            assertThat(result).hasStatusOk();

            TokenValidationResponse expectedResponse = new TokenValidationResponse(null, null, null, false, Set.of());

            assertThat(result)
                    .bodyJson()
                    .convertTo(TokenValidationResponse.class)
                    .usingRecursiveComparison()
                    .isEqualTo(expectedResponse);
        }
    }

    @Nested
    class TokenRefreshTest {

        @Test
        void refreshShouldReturnNewTokensWhenRefreshTokenIsValid() {
            TokenResponse tokens = getValidTokens(existingUserRequest.login(), existingUserRequest.password(), null);
            MvcTestResult result = performPost("/refresh", new TokenRequest(tokens.refreshToken()));

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson().hasPath("$.accessToken").hasPath("$.refreshToken");
        }

        @Test
        void refreshShouldReturnUnauthorizedWhenRefreshTokenIsInvalid() {
            MvcTestResult result = performPost("/refresh", new TokenRequest("invalidToken"));

            assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }

    private MvcTestResult performPost(String endpoint, Object body) {
        try {
            return mockMvcTester
                    .post()
                    .uri(BASE_URL + endpoint)
                    .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_INTERNAL_SERVICE")))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(body))
                    .exchange();
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize request body", e);
        }
    }

    private TokenResponse getValidTokens(String username, String password, String otp) {
        var result = performPost("/login", new LoginRequest(username, password, otp));
        assertThat(result).hasStatusOk();
        try {
            return jsonMapper.readValue(result.getResponse().getContentAsString(), TokenResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize TokenResponse", e);
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
            int binary = ((hash[offset] & 0x7f) << 24)
                    | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8)
                    | (hash[offset + 3] & 0xff);

            int otp = binary % 1000000;
            return String.format("%06d", otp);
        } catch (Exception e) {
            throw new RuntimeException("Failed to automatically generate test TOTP", e);
        }
    }
}
