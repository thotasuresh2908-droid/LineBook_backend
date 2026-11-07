package com.linebook.auth.exception;

/** No valid login: wrong credentials, missing token, expired token, deactivated account. */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
