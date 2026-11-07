package com.linebook.auth.dto;

import com.linebook.auth.domain.AppUser;
import com.linebook.auth.domain.UserRole;
import java.time.LocalDateTime;

/** A login account as the UI sees it. The password hash is never sent. */
public record UserResponse(
        String id,
        String email,
        String fullName,
        UserRole role,
        String agentId,
        boolean active,
        LocalDateTime lastLoginAt) {

    public static UserResponse from(AppUser u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getFullName(), u.getRole(),
                u.getAgentId(), u.isActive(), u.getLastLoginAt());
    }
}
