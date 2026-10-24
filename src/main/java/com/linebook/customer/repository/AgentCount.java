package com.linebook.customer.repository;

/** Number of customers of one agent (with a given connection status). */
public record AgentCount(String agentId, Long count) {
}
