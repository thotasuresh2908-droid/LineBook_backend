package com.linebook.collection.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

/** JSON values are lowercase: "cash" or "upi". */
public enum PayMethod {
    CASH, UPI;

    @JsonValue
    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }

    @JsonCreator
    public static PayMethod from(String raw) {
        return PayMethod.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    }
}
