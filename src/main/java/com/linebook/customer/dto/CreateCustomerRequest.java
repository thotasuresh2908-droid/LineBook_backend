package com.linebook.customer.dto;

import com.linebook.customer.domain.ConnectionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * The "Add customer" form. Optional fields default as in the UI: status active, recurring on, generate this
 * month's bill on, no STB/VC (generated), first package when none/unknown is given.
 * {@code billingDay} is optional (1-28, default 1).
 */
public record CreateCustomerRequest(
        @NotBlank @Schema(example = "Ravi Kumar") String name,
        @NotBlank @Schema(description = "Digits only are kept, at most 10", example = "9848011223") String phone,
        String address,
        @NotBlank @Schema(example = "Ameerpet") String area,
        String stbNo,
        String vcNo,
        @Schema(description = "Unknown or missing = the first package", example = "pkg-family") String packageId,
        @NotBlank @Schema(description = "Must be an existing agent", example = "ag-01") String agentId,
        ConnectionStatus status,
        Boolean recurring,
        Boolean generateBill,
        @Min(1) @Max(28) @Schema(description = "Day of the month the bill is due, 1-28 (default 1)", example = "10") Integer billingDay) {
}
