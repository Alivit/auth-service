package com.minispring.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import static com.minispring.authservice.util.ValidationPattern.LOGIN_PATTERN;
import static com.minispring.authservice.util.ValidationPattern.PASSWORD_PATTERN;

public record RegisterRequestDto (

        @NotBlank
        @Size(min = 2, max = 20, message = "Login must be between 2 and 20 characters long")
        @Pattern(regexp = LOGIN_PATTERN, message = "Login is not valid")
        String login,

        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(min = 2, max = 20, message = "Password must be between 2 and 20 characters long")
        @Pattern(regexp = PASSWORD_PATTERN, message = "Password is not valid")
        String password
){
}
