package com.linebook.collection.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Pure collection rules, a direct port of the LineBook front end (collect and nextReceiptNo in lib/store.ts).
 * No Spring, no I/O.
 */
public final class CollectionRules {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private CollectionRules() {
    }

    /** Collections are taken in whole rupees: Math.round(amount). */
    public static BigDecimal roundAmount(BigDecimal amount) {
        return amount.setScale(0, RoundingMode.HALF_UP);
    }

    /** Receipt number prefix for a day: "RC-yyyyMMdd-". */
    public static String receiptPrefix(LocalDate day) {
        return "RC-" + day.format(DAY) + "-";
    }

    /** Receipt number: prefix + (receipts already issued that day + 1), padded to 4 digits. */
    public static String receiptNo(LocalDate day, long seq) {
        return receiptPrefix(day) + String.format("%04d", seq);
    }

    /** agentId: the one entered, else the customer's own agent, else the default ("ag-01"). */
    public static String resolveAgent(String requested, String customerAgent, String fallback) {
        if (requested != null && !requested.isBlank()) {
            return requested.trim();
        }
        if (customerAgent != null && !customerAgent.isBlank()) {
            return customerAgent;
        }
        return fallback;
    }
}
