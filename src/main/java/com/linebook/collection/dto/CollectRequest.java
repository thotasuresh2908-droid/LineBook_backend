package com.linebook.collection.dto;

import com.linebook.collection.domain.PayMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * The "Record collection" form. Only billId and amount are required; method defaults to cash and the agent to the
 * customer's own agent (then "ag-01"), as in the UI. The agent's name on the receipt is looked up from the agent.
 * {@code collectionId} is optional: send the same one when retrying a request and the collection is never
 * recorded or applied to the bill twice.
 */
public record CollectRequest(
        @NotBlank String billId,
        @NotNull BigDecimal amount,
        PayMethod method,
        String agentId,
        @Size(max = 500) String notes,
        @Size(max = 64) String collectionId) {
}
