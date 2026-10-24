package com.linebook.collection.dto;

import com.linebook.collection.domain.CollectionRecord;
import com.linebook.collection.domain.PayMethod;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** The front end's Collection, plus the bill / customer details copied onto the receipt. */
public record CollectionResponse(
        String id,
        String receiptNo,
        String billId,
        String billNo,
        String customerId,
        String customerName,
        String accountNo,
        String stbNo,
        String agentId,
        String agentName,
        BigDecimal amount,
        PayMethod method,
        LocalDateTime collectedAt,
        String notes) {

    public static CollectionResponse from(CollectionRecord c) {
        return new CollectionResponse(c.getId(), c.getReceiptNo(), c.getBillId(), c.getBillNo(), c.getCustomerId(),
                c.getCustomerName(), c.getAccountNo(), c.getStbNo(), c.getAgentId(), c.getAgentName(), c.getAmount(),
                c.getMethod(), c.getCollectedAt(), c.getNotes());
    }
}
