package com.linebook.dashboard.dto;

import java.math.BigDecimal;

/**
 * One card on the Agents page. {@code activeAccounts} = the agent's active connections; {@code today*} = collected on
 * the day by this agent; {@code cycle*} = the bills of the agent's customers in the cycle (billed, paid, still pending,
 * and the paid share as a whole percent).
 */
public record AgentOverview(
        String id,
        String name,
        String phone,
        String area,
        boolean active,
        long activeAccounts,
        BigDecimal todayAmount,
        int todayCount,
        BigDecimal cycleBilled,
        BigDecimal cycleCollected,
        BigDecimal cyclePending,
        int cyclePercent) {
}
