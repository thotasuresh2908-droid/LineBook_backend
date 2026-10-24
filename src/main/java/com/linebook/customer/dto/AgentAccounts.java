package com.linebook.customer.dto;

/** How many connections of a given status one agent looks after. */
public record AgentAccounts(String agentId, long count) {
}
