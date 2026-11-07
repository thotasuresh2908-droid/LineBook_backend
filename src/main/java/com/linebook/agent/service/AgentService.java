package com.linebook.agent.service;

import com.linebook.agent.domain.Agent;
import com.linebook.agent.domain.AgentRules;
import com.linebook.agent.dto.AgentResponse;
import com.linebook.agent.dto.CreateAgentRequest;
import com.linebook.agent.dto.UpdateAgentRequest;
import com.linebook.agent.exception.AgentNotFoundException;
import com.linebook.agent.exception.InvalidAgentException;
import com.linebook.agent.repository.AgentRepository;
import com.linebook.collection.repository.CollectionRepository;
import com.linebook.customer.repository.CustomerRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Agent master data: list (the dropdowns), lookup (receipts), create, edit, activate / deactivate
 * and delete.
 *
 * <p>Creating an agent can also create that agent's login in the same transaction, so "Add agent"
 * on the Agents page is one step. Deleting an agent removes their login too, but only when nothing
 * in the books points at them, so bills and issued receipts can never be orphaned.
 */
@Service
public class AgentService {

    private final AgentRepository agents;
    private final CustomerRepository customers;
    private final CollectionRepository collections;
    /** Lazy, because AuthService depends on AgentService; ObjectProvider breaks the cycle. */
    private final ObjectProvider<com.linebook.auth.service.AuthService> auth;

    public AgentService(AgentRepository agents,
                        CustomerRepository customers,
                        CollectionRepository collections,
                        ObjectProvider<com.linebook.auth.service.AuthService> auth) {
        this.agents = agents;
        this.customers = customers;
        this.collections = collections;
        this.auth = auth;
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

    /** Adds the agent and, when an email and password are given, their login in the same transaction. */
    @Transactional
    public AgentResponse create(CreateAgentRequest request) {
        long seq = agents.maxSeq() + 1;
        Agent agent = agents.save(new Agent(
                AgentRules.agentId(seq),
                request.name().trim(),
                AgentRules.normalizePhone(request.phone()),
                request.area().trim(),
                true));

        boolean wantsLogin = notBlank(request.loginEmail()) || notBlank(request.loginPassword());
        if (wantsLogin) {
            if (!notBlank(request.loginEmail()) || !notBlank(request.loginPassword())) {
                throw new InvalidAgentException("Give both a login email and a password, or leave both empty");
            }
            auth.getObject().createForAgent(
                    request.loginEmail(), agent.getName(), request.loginPassword(), agent.getId());
        }
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

    /**
     * Permanently removes an agent and their login.
     *
     * <p>Refused while any customer is assigned to them or any receipt was issued in their name:
     * deleting then would break the receipts and the bill history. The message tells the person what
     * to do instead, which is to reassign those customers or simply deactivate the agent.
     */
    @Transactional
    public void delete(String id) {
        Agent agent = findEntity(id);

        long assigned = customers.countByAgentId(id);
        if (assigned > 0) {
            throw new InvalidAgentException(agent.getName() + " still has " + assigned
                    + " customer(s). Move them to another agent first, or deactivate this agent instead.");
        }
        long receipts = collections.countByAgentId(id);
        if (receipts > 0) {
            throw new InvalidAgentException(agent.getName() + " has " + receipts
                    + " receipt(s) in the books, so the record must be kept. Deactivate this agent instead;"
                    + " they will disappear from the dropdowns.");
        }

        auth.getObject().deleteForAgent(id);
        agents.delete(agent);
    }

    private Agent findEntity(String id) {
        return agents.findById(id).orElseThrow(() -> new AgentNotFoundException(id));
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
