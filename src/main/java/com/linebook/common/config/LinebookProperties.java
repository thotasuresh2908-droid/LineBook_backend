package com.linebook.common.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "linebook")
public record LinebookProperties(
        @DefaultValue("Asia/Kolkata") String zone,
        @DefaultValue("Surya Cable Network") String operatorName,
        @DefaultValue("ag-01") String defaultAgentId,
        @DefaultValue("true") boolean currentCycleOnly,
        @DefaultValue Cors cors,
        @DefaultValue Auth auth) {

    public record Cors(
            @DefaultValue({"http://localhost:8080", "http://localhost:5173"}) List<String> allowedOrigins,
            @DefaultValue("1h") Duration maxAge) {
    }

    /**
     * Sign-in settings.
     *
     * <p>{@code secret} signs the tokens: set a long random value in production
     * ({@code LINEBOOK_AUTH_SECRET}). Changing it signs everyone out, which is also how you revoke
     * every token at once.
     *
     * <p>{@code adminEmail} / {@code adminPassword} create the first admin login on a fresh
     * database (see AdminBootstrap); they are ignored once an admin exists.
     */
    public record Auth(
            @DefaultValue("change-me-linebook-dev-secret-do-not-use-in-production") String secret,
            @DefaultValue("12h") Duration tokenTtl,
            @DefaultValue("") String adminEmail,
            @DefaultValue("") String adminPassword,
            @DefaultValue("LineBook Admin") String adminName) {
    }
}
