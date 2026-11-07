package com.linebook.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Create a login. Admin only.
 *
 * <p>{@code role} is ADMIN or AGENT (default AGENT). For AGENT, {@code agentId} must name an
 * existing agent that has no login yet.
 */
public record CreateUserRequest(
        @NotBlank(message = "is required") @Email(message = "must be a valid email address") String email,
        @NotBlank(message = "is required") String fullName,
        @NotBlank(message = "is required") @Size(min = 6, message = "must be at least 6 characters") String password,
        String role,
        String agentId) {
}
