package com.linebook.auth.exception;

/** The login account itself is not valid: duplicate email, unknown agent, agent already has a login. */
public class InvalidUserException extends RuntimeException {

    public InvalidUserException(String message) {
        super(message);
    }
}
