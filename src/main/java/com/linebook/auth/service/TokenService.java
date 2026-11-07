package com.linebook.auth.service;

import com.linebook.auth.exception.UnauthorizedException;
import com.linebook.common.config.LinebookProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * Stateless sign-in tokens, signed with HMAC-SHA256 from the JDK, so no JWT library is needed.
 *
 * <p>Format: {@code base64url(payload).base64url(signature)} where the payload is
 * {@code userId|role|agentId|expiryEpochSeconds}. Anyone can read a token, nobody can forge one
 * without the server secret, and a tampered or expired token is rejected.
 *
 * <p>Set a long, random {@code LINEBOOK_AUTH_SECRET} in production: changing it signs everyone out,
 * which is also how you revoke every token at once.
 */
@Component
public class TokenService {

    private static final String ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    /** What a verified token tells us about the caller. */
    public record Claims(String userId, String role, String agentId, LocalDateTime expiresAt) {
    }

    private final byte[] secret;
    private final long ttlSeconds;
    private final Clock clock;
    private final ZoneId zone;

    public TokenService(LinebookProperties props, Clock clock) {
        this.secret = props.auth().secret().getBytes(StandardCharsets.UTF_8);
        this.ttlSeconds = props.auth().tokenTtl().toSeconds();
        this.clock = clock;
        this.zone = clock.getZone();
    }

    /** Issue a token for a user. {@code agentId} may be null for an admin. */
    public String issue(String userId, String role, String agentId) {
        long expiry = clock.instant().getEpochSecond() + ttlSeconds;
        String payload = userId + "|" + role + "|" + (agentId == null ? "" : agentId) + "|" + expiry;
        String encoded = ENCODER.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return encoded + "." + ENCODER.encodeToString(sign(encoded));
    }

    /** When the token issued right now would run out; shown to the UI so it can sign out quietly. */
    public LocalDateTime expiryOfNewToken() {
        return LocalDateTime.ofInstant(clock.instant().plusSeconds(ttlSeconds), zone);
    }

    /** Verify the signature and the expiry. Throws {@link UnauthorizedException} on anything wrong. */
    public Claims verify(String token) {
        if (token == null || token.isBlank()) {
            throw new UnauthorizedException("Sign in to continue");
        }
        int dot = token.indexOf('.');
        if (dot <= 0 || dot == token.length() - 1) {
            throw new UnauthorizedException("Your session is not valid; please sign in again");
        }
        String encoded = token.substring(0, dot);
        byte[] presented;
        try {
            presented = DECODER.decode(token.substring(dot + 1));
        } catch (IllegalArgumentException ex) {
            throw new UnauthorizedException("Your session is not valid; please sign in again");
        }
        if (!MessageDigest.isEqual(sign(encoded), presented)) {
            throw new UnauthorizedException("Your session is not valid; please sign in again");
        }

        String payload = new String(DECODER.decode(encoded), StandardCharsets.UTF_8);
        String[] parts = payload.split("\\|", -1);
        if (parts.length != 4) {
            throw new UnauthorizedException("Your session is not valid; please sign in again");
        }
        long expiry;
        try {
            expiry = Long.parseLong(parts[3]);
        } catch (NumberFormatException ex) {
            throw new UnauthorizedException("Your session is not valid; please sign in again");
        }
        if (clock.instant().getEpochSecond() >= expiry) {
            throw new UnauthorizedException("Your session has timed out; please sign in again");
        }
        String agentId = parts[2].isBlank() ? null : parts[2];
        return new Claims(parts[0], parts[1], agentId,
                LocalDateTime.ofInstant(java.time.Instant.ofEpochSecond(expiry), zone));
    }

    private byte[] sign(String encodedPayload) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret, ALGORITHM));
            return mac.doFinal(encodedPayload.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("Token signing is unavailable on this JVM", ex);
        }
    }
}
