package com.linebook.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.linebook.auth.exception.UnauthorizedException;
import com.linebook.auth.service.PasswordHasher;
import com.linebook.auth.service.TokenService;
import com.linebook.common.config.LinebookProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The two pieces the sign-in rests on: the password hash and the token signature. */
class AuthSecurityTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    private static final Instant NOW = Instant.parse("2026-10-12T06:00:00Z");

    private static LinebookProperties props(String secret, Duration ttl) {
        return new LinebookProperties("Asia/Kolkata", "Surya Cable Network", "ag-01", true,
                new LinebookProperties.Cors(List.of("http://localhost:8080"), Duration.ofHours(1)),
                new LinebookProperties.Auth(secret, ttl, "", "", "LineBook Admin"));
    }

    // ------------------------------------------------------------------ passwords

    @Test
    void theStoredHashIsNotThePasswordAndIsDifferentEveryTime() {
        PasswordHasher hasher = new PasswordHasher();

        String first = hasher.hash("secret123");
        String second = hasher.hash("secret123");

        assertThat(first).doesNotContain("secret123");
        assertThat(first).isNotEqualTo(second);   // a fresh salt each time
        assertThat(hasher.matches("secret123", first)).isTrue();
        assertThat(hasher.matches("secret123", second)).isTrue();
    }

    @Test
    void awrongPasswordDoesNotMatch() {
        PasswordHasher hasher = new PasswordHasher();
        String stored = hasher.hash("secret123");

        assertThat(hasher.matches("secret124", stored)).isFalse();
        assertThat(hasher.matches("", stored)).isFalse();
        assertThat(hasher.matches(null, stored)).isFalse();
    }

    @Test
    void aMalformedStoredValueIsRejectedRatherThanThrowing() {
        PasswordHasher hasher = new PasswordHasher();

        assertThat(hasher.matches("secret123", "not-a-hash")).isFalse();
        assertThat(hasher.matches("secret123", null)).isFalse();
    }

    // ------------------------------------------------------------------ tokens

    @Test
    void aTokenCarriesTheUserRoleAndAgent() {
        TokenService tokens = new TokenService(props("a-long-test-secret", Duration.ofHours(12)),
                Clock.fixed(NOW, ZONE));

        TokenService.Claims claims = tokens.verify(tokens.issue("us-0002", "AGENT", "ag-02"));

        assertThat(claims.userId()).isEqualTo("us-0002");
        assertThat(claims.role()).isEqualTo("AGENT");
        assertThat(claims.agentId()).isEqualTo("ag-02");
    }

    @Test
    void anAdminTokenHasNoAgent() {
        TokenService tokens = new TokenService(props("a-long-test-secret", Duration.ofHours(12)),
                Clock.fixed(NOW, ZONE));

        assertThat(tokens.verify(tokens.issue("us-0001", "ADMIN", null)).agentId()).isNull();
    }

    @Test
    void aTamperedTokenIsRejected() {
        TokenService tokens = new TokenService(props("a-long-test-secret", Duration.ofHours(12)),
                Clock.fixed(NOW, ZONE));
        String token = tokens.issue("us-0002", "AGENT", "ag-02");
        String tampered = token.substring(0, token.indexOf('.')) + "x" + token.substring(token.indexOf('.'));

        assertThatThrownBy(() -> tokens.verify(tampered)).isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> tokens.verify("rubbish")).isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> tokens.verify(null)).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void aTokenFromAnotherSecretIsRejected() {
        Clock clock = Clock.fixed(NOW, ZONE);
        String foreign = new TokenService(props("some-other-secret", Duration.ofHours(12)), clock)
                .issue("us-0001", "ADMIN", null);
        TokenService ours = new TokenService(props("a-long-test-secret", Duration.ofHours(12)), clock);

        assertThatThrownBy(() -> ours.verify(foreign)).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void anExpiredTokenIsRejected() {
        LinebookProperties props = props("a-long-test-secret", Duration.ofHours(1));
        String token = new TokenService(props, Clock.fixed(NOW, ZONE)).issue("us-0001", "ADMIN", null);

        // two hours later, with the same secret
        TokenService later = new TokenService(props, Clock.fixed(NOW.plusSeconds(7200), ZONE));

        assertThatThrownBy(() -> later.verify(token))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("timed out");
    }
}
