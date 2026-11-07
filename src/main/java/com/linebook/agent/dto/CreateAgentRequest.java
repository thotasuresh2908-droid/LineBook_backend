package com.linebook.agent.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * "Add agent". Name, phone and area are the agent's details.
 *
 * <p>{@code loginEmail} and {@code loginPassword} are optional: give both and the agent also gets a
 * login, so they can sign in and record their own collections. Leave them empty to add an agent
 * with no login (exactly how it worked before), and add the login later from the Agents page.
 */
public record CreateAgentRequest(
        @NotBlank(message = "is required") String name,
        @NotBlank(message = "is required") String phone,
        @NotBlank(message = "is required") String area,
        @Email(message = "must be a valid email address") String loginEmail,
        @Size(min = 6, message = "must be at least 6 characters") String loginPassword) {
}
