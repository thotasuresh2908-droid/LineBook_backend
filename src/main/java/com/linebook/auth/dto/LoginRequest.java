package com.linebook.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** What the login page posts. */
public record LoginRequest(
        @NotBlank(message = "is required") String email,
        @NotBlank(message = "is required") String password) {
}
