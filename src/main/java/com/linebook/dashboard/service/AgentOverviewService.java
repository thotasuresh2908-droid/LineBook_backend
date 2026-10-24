package com.linebook.dashboard.service;

import com.linebook.agent.domain.AgentRules;
import com.linebook.agent.dto.AgentResponse;
import com.linebook.agent.service.AgentService;
import com.linebook.billing.dto.AgentSummary;
import com.linebook.billing.service.BillingService;
import com.linebook.collection.dto.AgentTotal;
import com.linebook.collection.service.CollectionQueryService;
import com.linebook.customer.service.CustomerService;
import com.linebook.dashboard.dto.AgentOverview;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Agents page: for each agent, how many active accounts, what was collected on the day, and how the cycle book
 * stands. It only reads from the other modules and owns no tables.
 */
@Service
public class AgentOverviewService {

    private final AgentService agents;
    private final CustomerService customers;
    private final BillingService billing;
    private final CollectionQueryService collections;

    public AgentOverviewService(AgentService agents, CustomerService customers, BillingService billing,
                                CollectionQueryService collections) {
        this.agents = agents;
        this.customers = customers;
        this.billing = billing;
        this.collections = collections;
    }

    /** @param cycle yyyy-MM, null = current. @param date day for "today", null = today. */
    @Transactional(readOnly = true)
    public List<AgentOverview> overview(String cycle, LocalDate date, boolean includeInactive) {
        List<AgentResponse> list = agents.list(includeInactive);
        if (list.isEmpty()) {
            return List.of();
        }

        Map<String, Long> accounts = customers.activeAccountsByAgent();
        Map<String, AgentSummary> book = new HashMap<>();
        billing.summaryByAgent(cycle).forEach(s -> book.put(s.agentId(), s));
        Map<String, AgentTotal> today = new HashMap<>();
        collections.summary(date).byAgent().forEach(t -> today.put(t.agentId(), t));

        return list.stream().map(a -> {
            AgentSummary b = book.get(a.id());
            AgentTotal t = today.get(a.id());
            BigDecimal billed = b == null ? BigDecimal.ZERO : b.billed();
            BigDecimal collected = b == null ? BigDecimal.ZERO : b.collected();
            BigDecimal pending = b == null ? BigDecimal.ZERO : b.pending();
            return new AgentOverview(
                    a.id(), a.name(), a.phone(), a.area(), a.active(),
                    accounts.getOrDefault(a.id(), 0L),
                    t == null ? BigDecimal.ZERO : t.amount(),
                    t == null ? 0 : t.count(),
                    billed, collected, pending,
                    AgentRules.percentCollected(collected, billed));
        }).toList();
    }
}
