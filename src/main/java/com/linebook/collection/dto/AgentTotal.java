package com.linebook.collection.dto;

import java.math.BigDecimal;

public record AgentTotal(String agentId, BigDecimal amount, int count) {
}
