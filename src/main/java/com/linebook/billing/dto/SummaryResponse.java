package com.linebook.billing.dto;

import java.math.BigDecimal;

/** Cycle totals behind the dashboard KPIs: billed, received this cycle, pending, and status counts. */
public record SummaryResponse(
        String cycle,
        int billCount,
        BigDecimal generatedAmount,
        BigDecimal collectedAmount,
        BigDecimal pendingAmount,
        StatusCounts statusCounts) {
}
