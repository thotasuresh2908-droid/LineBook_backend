package com.linebook.auth.exception;

/** Signed in, but not allowed to do this (an agent trying an admin-only action). */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
