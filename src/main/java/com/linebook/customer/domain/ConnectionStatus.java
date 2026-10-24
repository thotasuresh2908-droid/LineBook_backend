package com.linebook.customer.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

/** JSON values are lowercase: "pending", "active", "suspended", "disconnected". */
public enum ConnectionStatus {
    PENDING, ACTIVE, SUSPENDED, DISCONNECTED;

    @JsonValue
    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }

    @JsonCreator
    public static ConnectionStatus from(String raw) {
        return ConnectionStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    }
}
