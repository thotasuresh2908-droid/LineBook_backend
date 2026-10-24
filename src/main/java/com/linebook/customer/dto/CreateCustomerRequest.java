package com.linebook.customer.dto;

import com.linebook.customer.domain.ConnectionStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * The "Add customer" form. Optional fields default as in the UI: status active, recurring on, generate this
 * month's bill on, no STB/VC (generated), first package when none/unknown is given.
 * {@code billingDay} is optional (1-28, default 1).
 */
public record CreateCustomerRequest(
        @NotBlank String name,
        @NotBlank String phone,
        String address,
        @NotBlank String area,
        String stbNo,
        String vcNo,
        String packageId,
        @NotBlank String agentId,
        ConnectionStatus status,
        Boolean recurring,
        Boolean generateBill,
        @Min(1) @Max(28) Integer billingDay) {
}
