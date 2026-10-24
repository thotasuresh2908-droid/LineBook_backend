package com.linebook.customer.dto;

import com.linebook.customer.domain.ConnectionStatus;
import com.linebook.customer.domain.Customer;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Same fields as the front end's Customer type (plus nothing else), so billing-service and the UI can share it. */
public record CustomerResponse(
        String id,
        String accountNo,
        String name,
        String phone,
        String address,
        String area,
        String stbNo,
        String vcNo,
        String packageId,
        BigDecimal monthlyRate,
        ConnectionStatus status,
        boolean recurring,
        int billingDay,
        String agentId,
        LocalDate since) {

    public static CustomerResponse from(Customer c) {
        return new CustomerResponse(c.getId(), c.getAccountNo(), c.getName(), c.getPhone(), c.getAddress(),
                c.getArea(), c.getStbNo(), c.getVcNo(), c.getPackageId(), c.getMonthlyRate(), c.getStatus(),
                c.isRecurring(), c.getBillingDay(), c.getAgentId(), c.getSince());
    }
}
