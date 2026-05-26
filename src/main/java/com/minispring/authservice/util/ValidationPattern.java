package com.minispring.authservice.util;

public final class ValidationPattern {
    public static final String LOGIN_PATTERN = "^[a-zA-Z][a-zA-Z0-9_]{2,20}$";
    public static final String PASSWORD_PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@.#$!%*?&])[A-Za-z\\d@.#$!%*?&]{8,20}$";

    ValidationPattern() {

    }
}
