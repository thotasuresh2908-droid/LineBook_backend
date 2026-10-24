package com.linebook.agent.dto;

import com.linebook.agent.domain.Agent;

/** The front end's Agent (id, name, phone, area) plus whether the agent is still active. */
public record AgentResponse(String id, String name, String phone, String area, boolean active) {

    public static AgentResponse from(Agent a) {
        return new AgentResponse(a.getId(), a.getName(), a.getPhone(), a.getArea(), a.isActive());
    }
}
