package com.linebook.auth.web;

import com.linebook.auth.domain.UserRole;

/** The caller of the request being handled, as read from the Authorization header. */
public record CurrentUser(String userId, String email, String fullName, UserRole role, String agentId) {

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    public boolean isAgent() {
        return role == UserRole.AGENT;
    }
}
