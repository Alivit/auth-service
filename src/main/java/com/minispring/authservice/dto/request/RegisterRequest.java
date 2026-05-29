package com.minispring.authservice.dto.request;

import static com.minispring.authservice.util.ValidationPattern.LOGIN_PATTERN;
import static com.minispring.authservice.util.ValidationPattern.PASSWORD_PATTERN;

import com.minispring.authservice.util.PasswordNotContainsLogin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@PasswordNotContainsLogin
public record RegisterRequest(
        @NotBlank(message = "Login is required")
        @Size(min = 2, max = 20, message = "Login must be between {min} and {max} characters long")
        @Pattern(regexp = LOGIN_PATTERN, message = "Login is not valid")
        String login,

        @NotBlank(message = "Email is required") @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 12, max = 64, message = "Password must be between {min} and {max} characters long")
        @Pattern(regexp = PASSWORD_PATTERN, message = "Password is not valid")
        String password) {
    public RegisterRequest {
        if (login != null) {
            login = login.trim().toLowerCase();
        }
        if (email != null) {
            email = email.trim().toLowerCase();
        }
    }
}
