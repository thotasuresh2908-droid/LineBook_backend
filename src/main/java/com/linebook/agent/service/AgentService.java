package com.linebook.agent.service;

import com.linebook.agent.domain.Agent;
import com.linebook.agent.domain.AgentRules;
import com.linebook.agent.dto.AgentResponse;
import com.linebook.agent.dto.CreateAgentRequest;
import com.linebook.agent.dto.UpdateAgentRequest;
import com.linebook.agent.exception.AgentNotFoundException;
import com.linebook.agent.repository.AgentRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Agent master data: list (the dropdowns), lookup (receipts), create, edit, activate / deactivate. */
@Service
public class AgentService {

    private final AgentRepository agents;

    public AgentService(AgentRepository agents) {
        this.agents = agents;
    }

    /** Active agents in creation order; with includeInactive, everyone. */
    @Transactional(readOnly = true)
    public List<AgentResponse> list(boolean includeInactive) {
        List<Agent> found = includeInactive ? agents.findAllByOrderBySeqAsc() : agents.findByActiveTrueOrderBySeqAsc();
        return found.stream().map(AgentResponse::from).toList();
    }

    /** Lookup for other modules (validating an agent id, naming the agent on a receipt). Includes inactive agents. */
    @Transactional(readOnly = true)
    public Optional<AgentResponse> find(String id) {
        return agents.findById(id).map(AgentResponse::from);
    }

    /** Works for inactive agents too, so old receipts can still show who collected. */
    @Transactional(readOnly = true)
    public AgentResponse get(String id) {
        return AgentResponse.from(findEntity(id));
    }

    @Transactional
    public AgentResponse create(CreateAgentRequest request) {
        long seq = agents.count() + 1;
        Agent agent = agents.save(new Agent(
                AgentRules.agentId(seq),
                request.name().trim(),
                AgentRules.normalizePhone(request.phone()),
                request.area().trim(),
                true));
        return AgentResponse.from(agent);
    }

    @Transactional
    public AgentResponse update(String id, UpdateAgentRequest request) {
        Agent agent = findEntity(id);
        agent.changeDetails(request.name().trim(), AgentRules.normalizePhone(request.phone()), request.area().trim());
        return AgentResponse.from(agents.save(agent));
    }

    @Transactional
    public AgentResponse setActive(String id, boolean active) {
        Agent agent = findEntity(id);
        agent.changeActive(active);
        return AgentResponse.from(agents.save(agent));
    }

    private Agent findEntity(String id) {
        return agents.findById(id).orElseThrow(() -> new AgentNotFoundException(id));
    }
}
