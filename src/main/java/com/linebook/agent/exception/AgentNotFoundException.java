package com.linebook.agent.exception;

public class AgentNotFoundException extends RuntimeException {
    public AgentNotFoundException(String id) {
        super("Agent not found: " + id);
    }
}
