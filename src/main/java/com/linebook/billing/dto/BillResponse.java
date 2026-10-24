package com.linebook.billing.dto;

import com.linebook.billing.domain.BillStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** An "enriched bill": the bill, what has been paid, what is pending, its derived status, and its customer. */
public record BillResponse(
        String id,
        String billNo,
        String customerId,
        String cycle,
        BigDecimal amount,
        LocalDateTime generatedAt,
        LocalDate dueDate,
        BigDecimal paid,
        BigDecimal pending,
        BillStatus status,
        CustomerSummary customer) {
}
