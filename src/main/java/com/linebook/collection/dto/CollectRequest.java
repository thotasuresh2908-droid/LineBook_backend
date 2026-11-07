package com.linebook.collection.dto;

import com.linebook.collection.domain.PayMethod;
import io.swagger.v3.oas.annotations.media.Schema;
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
        @NotBlank @Schema(example = "bl-2026-10-cu-3fa85f6412ab") String billId,
        @NotNull @Schema(description = "Rounded to a whole rupee; must be > 0 and within the pending balance", example = "299") BigDecimal amount,
        PayMethod method,
        @Schema(description = "Defaults to the customer's agent", example = "ag-01") String agentId,
        @Size(max = 500) String notes,
        @Size(max = 64) @Schema(description = "Your own id for this collection; sending it again never records the payment twice", example = "7f3a9c21-0b1d-4e6a-9d2e-5c8f1a2b3c4d") String collectionId) {
}
