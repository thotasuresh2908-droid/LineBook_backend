package com.linebook.collection.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** The Today page numbers: collected today, receipts today, and the per-agent breakdown. */
public record DaySummary(LocalDate date, int count, BigDecimal totalAmount, List<AgentTotal> byAgent) {
}
