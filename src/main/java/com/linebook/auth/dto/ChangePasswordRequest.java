package com.linebook.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Set a new password: your own (with the current one) or, for an admin, somebody else's. */
public record ChangePasswordRequest(
        String currentPassword,
        @NotBlank(message = "is required") @Size(min = 6, message = "must be at least 6 characters") String newPassword) {
}
