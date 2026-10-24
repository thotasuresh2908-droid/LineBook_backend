package com.linebook.collection.service;

import com.linebook.agent.dto.AgentResponse;
import com.linebook.agent.service.AgentService;
import com.linebook.billing.dto.BillResponse;
import com.linebook.billing.service.BillingService;
import com.linebook.collection.domain.CollectionRecord;
import com.linebook.collection.domain.CollectionRules;
import com.linebook.collection.domain.PayMethod;
import com.linebook.collection.dto.CollectRequest;
import com.linebook.collection.dto.CollectResult;
import com.linebook.collection.dto.CollectionResponse;
import com.linebook.collection.exception.InvalidCollectionException;
import com.linebook.collection.repository.CollectionRepository;
import com.linebook.common.config.LinebookProperties;
import com.linebook.customer.domain.ConnectionStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "Record collection". Behaviour matches the front end's collect():
 * whole-rupee amount, > 0 and within the pending balance, cash by default, the customer's agent by default,
 * and a numbered receipt.
 *
 * One transaction covers everything: the payment applied to the bill (the bill row is locked, so two simultaneous
 * collections cannot overpay), the receipt number and the collection row. If any step fails, nothing is saved.
 */
@Service
public class CollectionService {

    private final CollectionRepository collections;
    private final BillingService billing;
    private final AgentService agents;
    private final ReceiptNumbers receipts;
    private final LinebookProperties props;
    private final Clock clock;

    public CollectionService(CollectionRepository collections, BillingService billing, AgentService agents,
                             ReceiptNumbers receipts, LinebookProperties props, Clock clock) {
        this.collections = collections;
        this.billing = billing;
        this.agents = agents;
        this.receipts = receipts;
        this.props = props;
        this.clock = clock;
    }

    @Transactional
    public CollectResult collect(CollectRequest request) {
        String collectionId = blank(request.collectionId())
                ? "co-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12)
                : request.collectionId().trim();

        // Replay of a request that already went through.
        Optional<CollectionRecord> existing = collections.findById(collectionId);
        if (existing.isPresent()) {
            return new CollectResult(CollectionResponse.from(existing.get()), false);
        }

        BillResponse bill = billing.get(request.billId());
        ensureCollectable(bill);

        BigDecimal amount = CollectionRules.roundAmount(request.amount());
        if (amount.signum() <= 0 || amount.compareTo(bill.pending()) > 0) {
            throw new InvalidCollectionException(
                    "Enter a valid amount within the pending balance (pending: " + bill.pending().toPlainString() + ")");
        }

        String agentId = CollectionRules.resolveAgent(request.agentId(), bill.customer().agentId(), props.defaultAgentId());
        AgentResponse agent = agents.find(agentId)
                .orElseThrow(() -> new InvalidCollectionException("Unknown agent: " + agentId));

        // Locks the bill row and re-checks the pending balance; idempotent on collectionId.
        billing.recordPayment(bill.id(), collectionId, amount);

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate day = now.toLocalDate();
        String receiptNo = CollectionRules.receiptNo(day, receipts.next(day));

        CollectionRecord saved = collections.save(new CollectionRecord(
                collectionId,
                receiptNo,
                bill.id(),
                bill.billNo(),
                bill.customerId(),
                bill.customer().name(),
                bill.customer().accountNo(),
                bill.customer().stbNo(),
                agent.id(),
                agent.name(),
                amount,
                request.method() == null ? PayMethod.CASH : request.method(),
                now,
                request.notes() == null ? "" : request.notes().trim()));

        return new CollectResult(CollectionResponse.from(saved), true);
    }

    /** The Record-collection dialog only offers pending, current-cycle bills of connections that are not disconnected. */
    private void ensureCollectable(BillResponse bill) {
        if (bill.customer().status() == ConnectionStatus.DISCONNECTED) {
            throw new InvalidCollectionException("Collections cannot be recorded for a disconnected connection");
        }
        if (bill.pending().signum() <= 0) {
            throw new InvalidCollectionException("This bill has no pending balance");
        }
        if (props.currentCycleOnly() && !YearMonth.now(clock).toString().equals(bill.cycle())) {
            throw new InvalidCollectionException("Only current-cycle bills can be collected (bill cycle: " + bill.cycle() + ")");
        }
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
