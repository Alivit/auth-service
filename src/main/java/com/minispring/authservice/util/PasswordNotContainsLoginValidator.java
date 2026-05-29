package com.minispring.authservice.util;

import com.minispring.authservice.dto.LoginRequestDto;
import com.minispring.authservice.dto.RegisterRequestDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordNotContainsLoginValidator implements ConstraintValidator<PasswordNotContainsLogin, Object> {

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        String login = null;
        String password = null;

        if (value instanceof RegisterRequestDto dto) {
            login = dto.login();
            password = dto.password();
        } else if (value instanceof LoginRequestDto dto) {
            login = dto.login();
            password = dto.password();
        }

        if (login == null || password == null) {
            return true;
        }

        return !password.toLowerCase().contains(login.toLowerCase());
    }
}
