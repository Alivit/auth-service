package com.minispring.authservice.util;

public final class ValidationPattern {
    public static final String LOGIN_PATTERN = "^[a-zA-Z][a-zA-Z0-9_]{1,19}$";
    public static final String PASSWORD_PATTERN =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@.#$!%*?&])[A-Za-z\\d@.#$!%*?&]{12,64}$";
    public static final String OTP_PATTERN = "[0-9]{6}";

    ValidationPattern() {}
}
