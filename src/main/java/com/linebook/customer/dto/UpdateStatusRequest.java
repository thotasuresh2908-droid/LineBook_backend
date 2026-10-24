package com.linebook.customer.dto;

import com.linebook.customer.domain.ConnectionStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull ConnectionStatus status) {
}
