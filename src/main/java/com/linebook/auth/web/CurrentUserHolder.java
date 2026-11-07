package com.linebook.auth.web;

import com.linebook.auth.exception.ForbiddenException;
import com.linebook.auth.exception.UnauthorizedException;
import java.util.Optional;

/**
 * The signed-in caller for the thread handling the current request. {@link AuthFilter} sets it at
 * the start of a request and always clears it at the end, so nothing leaks between requests on a
 * pooled thread.
 */
public final class CurrentUserHolder {

    private static final ThreadLocal<CurrentUser> CURRENT = new ThreadLocal<>();

    private CurrentUserHolder() {
    }

    static void set(CurrentUser user) {
        CURRENT.set(user);
    }

    static void clear() {
        CURRENT.remove();
    }

    /** Empty on the public endpoints (login, health, Swagger). */
    public static Optional<CurrentUser> find() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static CurrentUser require() {
        CurrentUser user = CURRENT.get();
        if (user == null) {
            throw new UnauthorizedException("Sign in to continue");
        }
        return user;
    }

    public static CurrentUser requireAdmin() {
        CurrentUser user = require();
        if (!user.isAdmin()) {
            throw new ForbiddenException("Only an admin can do this");
        }
        return user;
    }
}
