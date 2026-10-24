package com.linebook.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAgentRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank String phone,
        @NotBlank @Size(max = 100) String area) {
}
