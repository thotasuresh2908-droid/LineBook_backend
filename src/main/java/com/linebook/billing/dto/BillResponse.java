package com.linebook.billing.dto;

import com.linebook.billing.domain.BillStatus;
import com.linebook.customer.dto.CustomerResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * An "enriched bill": the bill, what has been paid, what is pending, its derived status, and its customer (all of the
 * customer's details, so a bill can be shown or shared, e.g. as a WhatsApp bill image, without a second request).
 */
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
        CustomerResponse customer) {
}
