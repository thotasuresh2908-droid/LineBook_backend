package com.linebook.billing.dto;

import com.linebook.customer.domain.ConnectionStatus;

public record CustomerSummary(
        String id,
        String accountNo,
        String name,
        String phone,
        String area,
        String stbNo,
        ConnectionStatus status,
        String agentId) {
}
