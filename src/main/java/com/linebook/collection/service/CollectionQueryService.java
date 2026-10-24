package com.linebook.collection.service;

import com.linebook.collection.domain.CollectionRecord;
import com.linebook.collection.dto.AgentTotal;
import com.linebook.collection.dto.CollectionResponse;
import com.linebook.collection.dto.DailyTotal;
import com.linebook.collection.dto.DaySummary;
import com.linebook.collection.dto.ReceiptResponse;
import com.linebook.collection.exception.CollectionNotFoundException;
import com.linebook.collection.repository.CollectionRepository;
import com.linebook.collection.repository.CollectionSpecs;
import com.linebook.common.config.LinebookProperties;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read side: the Today page (receipts, totals, per-agent, 14-day chart) and receipts. */
@Service
@Transactional(readOnly = true)
public class CollectionQueryService {

    private static final int MAX_DAYS = 90;

    private final CollectionRepository collections;
    private final LinebookProperties props;
    private final Clock clock;

    public CollectionQueryService(CollectionRepository collections, LinebookProperties props, Clock clock) {
        this.collections = collections;
        this.props = props;
        this.clock = clock;
    }

    /** Collections between two dates (inclusive, default today), newest first. Optional agent / bill / customer filters. */
    public List<CollectionResponse> list(LocalDate from, LocalDate to, String agentId, String billId, String customerId) {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = from == null ? today : from;
        LocalDate end = to == null ? start : to;

        Specification<CollectionRecord> spec = CollectionSpecs.collectedBetween(
                start.atStartOfDay(), end.plusDays(1).atStartOfDay());
        if (!blank(agentId)) {
            spec = spec.and(CollectionSpecs.byAgent(agentId.trim()));
        }
        if (!blank(billId)) {
            spec = spec.and(CollectionSpecs.byBill(billId.trim()));
        }
        if (!blank(customerId)) {
            spec = spec.and(CollectionSpecs.byCustomer(customerId.trim()));
        }
        return collections.findAll(spec, Sort.by(Sort.Direction.DESC, "collectedAt")).stream()
                .map(CollectionResponse::from)
                .toList();
    }

    /** Collected on a day (default today): receipts count, total amount and the per-agent split. */
    public DaySummary summary(LocalDate dateOrNull) {
        LocalDate day = dateOrNull == null ? LocalDate.now(clock) : dateOrNull;
        List<CollectionRecord> rows = onDay(day);

        BigDecimal total = BigDecimal.ZERO;
        Map<String, BigDecimal> amountByAgent = new LinkedHashMap<>();
        Map<String, Integer> countByAgent = new LinkedHashMap<>();
        for (CollectionRecord c : rows) {
            total = total.add(c.getAmount());
            amountByAgent.merge(c.getAgentId(), c.getAmount(), BigDecimal::add);
            countByAgent.merge(c.getAgentId(), 1, Integer::sum);
        }
        List<AgentTotal> byAgent = new ArrayList<>();
        amountByAgent.forEach((agent, amount) -> byAgent.add(new AgentTotal(agent, amount, countByAgent.get(agent))));
        byAgent.sort(Comparator.comparing(AgentTotal::agentId));
        return new DaySummary(day, rows.size(), total, byAgent);
    }

    /** Collected per day for the last N days ending today (default 14), oldest first, zero-filled: the Today chart. */
    public List<DailyTotal> daily(Integer daysOrNull) {
        int days = daysOrNull == null ? 14 : Math.max(1, Math.min(MAX_DAYS, daysOrNull));
        LocalDate today = LocalDate.now(clock);
        LocalDate first = today.minusDays(days - 1L);

        Map<LocalDate, BigDecimal> totals = new TreeMap<>();
        for (int i = 0; i < days; i++) {
            totals.put(first.plusDays(i), BigDecimal.ZERO);
        }
        List<CollectionRecord> rows = collections.findAll(
                CollectionSpecs.collectedBetween(first.atStartOfDay(), today.plusDays(1).atStartOfDay()));
        for (CollectionRecord c : rows) {
            totals.merge(c.getCollectedAt().toLocalDate(), c.getAmount(), BigDecimal::add);
        }
        return totals.entrySet().stream().map(e -> new DailyTotal(e.getKey(), e.getValue())).toList();
    }

    public CollectionResponse get(String id) {
        return CollectionResponse.from(find(id));
    }

    public ReceiptResponse receipt(String id) {
        return ReceiptResponse.from(find(id), props.operatorName());
    }

    private List<CollectionRecord> onDay(LocalDate day) {
        return collections.findAll(CollectionSpecs.collectedBetween(day.atStartOfDay(), day.plusDays(1).atStartOfDay()));
    }

    private CollectionRecord find(String id) {
        return collections.findById(id).orElseThrow(() -> new CollectionNotFoundException(id));
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
