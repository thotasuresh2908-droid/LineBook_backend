package com.linebook.billing.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

/** Derived, never stored. JSON values are lowercase ("paid", "partial", ...). */
public enum BillStatus {
    PAID, PARTIAL, PENDING, OVERDUE;

    @JsonValue
    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }

    @JsonCreator
    public static BillStatus from(String raw) {
        return BillStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    }
}
