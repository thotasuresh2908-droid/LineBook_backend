package com.linebook.auth.dto;

import java.time.LocalDateTime;

/**
 * What a successful login returns. {@code token} goes into the Authorization header of every
 * later request; {@code user} is what the header of the app shows.
 */
public record LoginResponse(String token, LocalDateTime expiresAt, UserResponse user) {
}
