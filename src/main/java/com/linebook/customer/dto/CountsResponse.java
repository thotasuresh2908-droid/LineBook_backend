package com.linebook.customer.dto;

/** Header counts on the Customers page, plus "recurringActive" (active and recurring) from the Today page. */
public record CountsResponse(
        long total,
        long active,
        long pending,
        long suspended,
        long disconnected,
        long recurringActive) {
}
