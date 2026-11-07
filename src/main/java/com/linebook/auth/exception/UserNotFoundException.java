package com.linebook.auth.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String id) {
        super("Login not found: " + id);
    }
}
