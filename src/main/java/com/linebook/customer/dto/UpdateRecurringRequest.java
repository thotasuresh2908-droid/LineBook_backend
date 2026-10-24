package com.linebook.customer.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateRecurringRequest(@NotNull Boolean recurring) {
}
