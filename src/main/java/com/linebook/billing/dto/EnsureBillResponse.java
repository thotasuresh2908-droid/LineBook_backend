package com.linebook.billing.dto;

/** {@code bill} is null when the customer is not billable (not active, or recurring is off). */
public record EnsureBillResponse(boolean created, BillResponse bill) {
}
