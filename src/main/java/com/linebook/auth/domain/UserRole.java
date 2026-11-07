package com.linebook.auth.domain;

/** What a signed-in person is allowed to do. */
public enum UserRole {

    /** Owner / office staff: everything, including adding and deleting agents and their logins. */
    ADMIN,

    /** A collection agent in the field: records collections under their own name only. */
    AGENT;

    public static UserRole from(String raw) {
        if (raw == null || raw.isBlank()) {
            return AGENT;
        }
        return UserRole.valueOf(raw.trim().toUpperCase());
    }
}
