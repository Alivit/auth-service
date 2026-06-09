package com.minispring.authservice.exception;

public final class ExceptionAnswer {
    public static final String USER_NOT_FOUND = "User with id %s not found";
    public static final String USER_EXIST = "This email %s or login %s already exists";
    public static final String INVALID_CREDENTIALS = "This password or login not valid";
    public static final String SESSION_EXPIRED = "Your session has expired, please log in again";
}
