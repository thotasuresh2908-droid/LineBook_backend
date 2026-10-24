package com.linebook.collection.dto;

import com.linebook.collection.domain.CollectionRecord;
import com.linebook.collection.domain.PayMethod;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Everything the Receipt dialog prints. */
public record ReceiptResponse(
        String operatorName,
        String receiptNo,
        LocalDateTime collectedAt,
        String customerName,
        String accountNo,
        String billNo,
        String stbNo,
        BigDecimal amount,
        PayMethod method,
        String agentId,
        String agentName) {

    public static ReceiptResponse from(CollectionRecord c, String operatorName) {
        return new ReceiptResponse(operatorName, c.getReceiptNo(), c.getCollectedAt(), c.getCustomerName(),
                c.getAccountNo(), c.getBillNo(), c.getStbNo(), c.getAmount(), c.getMethod(), c.getAgentId(),
                c.getAgentName());
    }
}
