package com.linebook.auth.dto;

import jakarta.validation.constraints.NotNull;

/** Turn a login on or off without deleting it. */
public record UpdateUserActiveRequest(@NotNull(message = "is required") Boolean active) {
}
