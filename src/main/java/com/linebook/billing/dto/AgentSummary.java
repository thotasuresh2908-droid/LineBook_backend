package com.linebook.billing.dto;

import java.math.BigDecimal;

/** The cycle's bills of one agent's customers: how many, billed, collected so far, still pending. */
public record AgentSummary(String agentId, int billCount, BigDecimal billed, BigDecimal collected, BigDecimal pending) {
}
