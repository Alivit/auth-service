package com.minispring.authservice.dto.request;

import static com.minispring.authservice.util.ValidationPattern.LOGIN_PATTERN;
import static com.minispring.authservice.util.ValidationPattern.OTP_PATTERN;
import static com.minispring.authservice.util.ValidationPattern.PASSWORD_PATTERN;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Login is required")
        @Size(min = 2, max = 20, message = "Login must be between {min} and {max} characters long")
        @Pattern(regexp = LOGIN_PATTERN, message = "Login is not valid")
        String login,

        @NotBlank(message = "Password is required")
        @Size(min = 12, max = 64, message = "Password must be between {min} and {max} characters long")
        @Pattern(regexp = PASSWORD_PATTERN, message = "Password is not valid")
        String password,

        @Pattern(regexp = OTP_PATTERN, message = "OTP code must be exactly 6 digits")
        String otpCode) {
    public LoginRequest {
        if (login != null) {
            login = login.trim().toLowerCase();
        }
        if (otpCode != null) {
            otpCode = otpCode.trim();
        }
    }
}
