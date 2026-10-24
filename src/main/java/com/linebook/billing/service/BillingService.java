package com.linebook.billing.service;

import com.linebook.billing.domain.Bill;
import com.linebook.billing.domain.BillPayment;
import com.linebook.billing.domain.BillStatus;
import com.linebook.billing.domain.BillingRules;
import com.linebook.billing.dto.BillResponse;
import com.linebook.billing.dto.AgentSummary;
import com.linebook.billing.dto.CustomerSummary;
import com.linebook.billing.dto.EnsureBillResponse;
import com.linebook.billing.dto.GenerateResponse;
import com.linebook.billing.dto.StatusCounts;
import com.linebook.billing.dto.SummaryResponse;
import com.linebook.billing.exception.BillNotFoundException;
import com.linebook.billing.exception.InvalidPaymentException;
import com.linebook.billing.repository.BillPaymentRepository;
import com.linebook.billing.repository.BillRepository;
import com.linebook.billing.repository.PaidTotal;
import com.linebook.customer.domain.ConnectionStatus;
import com.linebook.customer.dto.CustomerResponse;
import com.linebook.customer.exception.CustomerNotFoundException;
import com.linebook.customer.service.CustomerService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Billing use cases. Behaviour matches the LineBook front-end store:
 * generateCycle, the auto-create-current-bill rule, enrichBills and collect's amount checks.
 */
@Service
public class BillingService {

    private static final int COLLECTABLE_LIMIT = 8;

    private final BillRepository bills;
    private final BillPaymentRepository payments;
    private final CustomerService customers;
    private final Clock clock;

    public BillingService(BillRepository bills, BillPaymentRepository payments,
                          CustomerService customers, Clock clock) {
        this.bills = bills;
        this.payments = payments;
        this.customers = customers;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ generation

    /**
     * "Run monthly billing": create one bill for every billable customer (active + recurring)
     * that has none in the cycle yet. Idempotent. Defaults to the current cycle.
     */
    @Transactional
    public GenerateResponse generateCycle(String cycleOrNull) {
        YearMonth target = resolveCycle(cycleOrNull);
        String cycle = target.toString();
        Set<String> existing = bills.findCustomerIdsByCycle(cycle);

        List<Bill> created = new ArrayList<>();
        for (CustomerResponse customer : customers.findAll()) {
            if (BillingRules.isBillable(customer.status(), customer.recurring())
                    && !existing.contains(customer.id())) {
                created.add(BillingRules.makeBill(customer, target, existing.size() + created.size() + 1));
            }
        }
        if (!created.isEmpty()) {
            bills.saveAll(created);
        }
        return new GenerateResponse(created.size(), cycle);
    }

    /**
     * The "customer became billable" rule (add customer with generateBill, activate, or turn recurring on):
     * create this month's bill if the customer is billable and has none yet.
     */
    @Transactional
    public EnsureBillResponse ensureCurrentCycleBill(String customerId) {
        CustomerResponse customer = customers.find(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
        YearMonth cycle = YearMonth.now(clock);
        String cycleKey = cycle.toString();
        LocalDate today = LocalDate.now(clock);

        var existing = bills.findByCustomerIdAndCycle(customerId, cycleKey);
        if (existing.isPresent()) {
            return new EnsureBillResponse(false, toResponse(existing.get(), customer, paidOn(existing.get().getId()), today));
        }
        if (!BillingRules.isBillable(customer.status(), customer.recurring())) {
            return new EnsureBillResponse(false, null);
        }
        int seq = (int) bills.countByCycle(cycleKey) + 1;
        Bill bill = bills.save(BillingRules.makeBill(customer, cycle, seq));
        return new EnsureBillResponse(true, toResponse(bill, customer, BigDecimal.ZERO, today));
    }

    // ------------------------------------------------------------------ queries

    @Transactional(readOnly = true)
    public List<String> cycles() {
        return bills.findDistinctCycles();
    }

    /** Bills page: one cycle, optionally filtered by status, agent and a free-text search. */
    @Transactional(readOnly = true)
    public List<BillResponse> list(String cycleOrNull, String statusOrNull, String agentIdOrNull, String q) {
        String cycle = resolveCycle(cycleOrNull).toString();
        BillStatus status = parseStatus(statusOrNull);
        String agentId = isAll(agentIdOrNull) ? null : agentIdOrNull;
        String query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);

        return enrichCycle(cycle).stream()
                .filter(b -> agentId == null || agentId.equals(b.customer().agentId()))
                .filter(b -> status == null || b.status() == status)
                .filter(b -> query.isEmpty() || searchText(b, false).contains(query))
                .sorted(Comparator.comparing(BillResponse::billNo))
                .toList();
    }

    @Transactional(readOnly = true)
    public BillResponse get(String billId) {
        Bill bill = bills.findById(billId).orElseThrow(() -> new BillNotFoundException(billId));
        CustomerResponse customer = customers.find(bill.getCustomerId())
                .orElseThrow(() -> new CustomerNotFoundException(bill.getCustomerId()));
        return toResponse(bill, customer, paidOn(billId), LocalDate.now(clock));
    }

    /** Customer detail dialog: all of one customer's bills across cycles, newest cycle first. */
    @Transactional(readOnly = true)
    public List<BillResponse> listForCustomer(String customerId) {
        CustomerResponse customer = customers.find(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
        LocalDate today = LocalDate.now(clock);
        return bills.findByCustomerIdOrderByCycleDesc(customerId).stream()
                .map(bill -> toResponse(bill, customer, paidOn(bill.getId()), today))
                .toList();
    }

    /** Dashboard KPIs for a cycle (billed, received this cycle, pending, counts per status). */
    @Transactional(readOnly = true)
    public SummaryResponse summary(String cycleOrNull) {
        String cycle = resolveCycle(cycleOrNull).toString();
        List<BillResponse> rows = enrichCycle(cycle);
        BigDecimal generated = rows.stream().map(BillResponse::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal collected = rows.stream().map(BillResponse::paid).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pending = rows.stream().map(BillResponse::pending).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new SummaryResponse(cycle, rows.size(), generated, collected, pending, new StatusCounts(
                count(rows, BillStatus.PAID),
                count(rows, BillStatus.PARTIAL),
                count(rows, BillStatus.PENDING),
                count(rows, BillStatus.OVERDUE)));
    }

    /**
     * Per agent: how the cycle's bills of that agent's customers stand (billed, collected, pending). Agents with no
     * bills in the cycle are absent. Feeds the Agents page.
     */
    @Transactional(readOnly = true)
    public List<AgentSummary> summaryByAgent(String cycleOrNull) {
        String cycle = resolveCycle(cycleOrNull).toString();
        Map<String, List<BillResponse>> byAgent = new TreeMap<>();
        for (BillResponse bill : enrichCycle(cycle)) {
            byAgent.computeIfAbsent(nz(bill.customer().agentId()), k -> new ArrayList<>()).add(bill);
        }
        List<AgentSummary> result = new ArrayList<>();
        byAgent.forEach((agentId, rows) -> result.add(new AgentSummary(
                agentId,
                rows.size(),
                rows.stream().map(BillResponse::amount).reduce(BigDecimal.ZERO, BigDecimal::add),
                rows.stream().map(BillResponse::paid).reduce(BigDecimal.ZERO, BigDecimal::add),
                rows.stream().map(BillResponse::pending).reduce(BigDecimal.ZERO, BigDecimal::add))));
        return result;
    }

    /**
     * "Record collection" bill picker: current-cycle bills with a pending balance whose customer is not
     * disconnected, matched on bill no, name, account, STB or phone, capped at 8.
     */
    @Transactional(readOnly = true)
    public List<BillResponse> collectable(String q) {
        String query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        return enrichCycle(YearMonth.now(clock).toString()).stream()
                .filter(b -> b.pending().signum() > 0)
                .filter(b -> b.customer().status() != ConnectionStatus.DISCONNECTED)
                .filter(b -> query.isEmpty() || searchText(b, true).contains(query))
                .limit(COLLECTABLE_LIMIT)
                .toList();
    }

    // ------------------------------------------------------------------ payments

    /**
     * Apply a collection to a bill. Same rule as the store's collect(): the amount is rounded to a whole
     * rupee and must be > 0 and no more than the pending balance. Replaying a collectionId is a no-op.
     */
    @Transactional
    public BillResponse recordPayment(String billId, String collectionId, BigDecimal rawAmount) {
        Bill bill = bills.findByIdForUpdate(billId).orElseThrow(() -> new BillNotFoundException(billId));
        CustomerResponse customer = customers.find(bill.getCustomerId())
                .orElseThrow(() -> new CustomerNotFoundException(bill.getCustomerId()));
        LocalDate today = LocalDate.now(clock);

        if (payments.existsByCollectionId(collectionId)) {
            return toResponse(bill, customer, paidOn(billId), today);
        }

        BigDecimal amount = rawAmount.setScale(0, RoundingMode.HALF_UP);
        BigDecimal pending = BillingRules.pending(bill.getAmount(), paidOn(billId));
        if (amount.signum() <= 0 || amount.compareTo(pending) > 0) {
            throw new InvalidPaymentException(
                    "Enter a valid amount within the pending balance (pending: " + pending.toPlainString() + ")");
        }

        payments.save(new BillPayment(billId, collectionId, amount, LocalDateTime.now(clock)));
        return toResponse(bill, customer, paidOn(billId), today);
    }

    // ------------------------------------------------------------------ helpers

    private List<BillResponse> enrichCycle(String cycle) {
        List<Bill> cycleBills = bills.findByCycleOrderByBillNoAsc(cycle);
        if (cycleBills.isEmpty()) {
            return List.of();
        }
        Map<String, BigDecimal> paid = new HashMap<>();
        for (PaidTotal total : payments.sumByCycle(cycle)) {
            paid.put(total.billId(), total.total());
        }
        Map<String, CustomerResponse> byId = customers
                .findByIds(cycleBills.stream().map(Bill::getCustomerId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(CustomerResponse::id, c -> c, (a, b) -> a));

        LocalDate today = LocalDate.now(clock);
        List<BillResponse> out = new ArrayList<>(cycleBills.size());
        for (Bill bill : cycleBills) {
            CustomerResponse customer = byId.get(bill.getCustomerId());
            if (customer == null) {
                continue; // like enrichBills in the UI: bills whose customer is unknown are skipped
            }
            out.add(toResponse(bill, customer, paid.getOrDefault(bill.getId(), BigDecimal.ZERO), today));
        }
        return out;
    }

    private BillResponse toResponse(Bill bill, CustomerResponse c, BigDecimal paid, LocalDate today) {
        return new BillResponse(
                bill.getId(),
                bill.getBillNo(),
                bill.getCustomerId(),
                bill.getCycle(),
                bill.getAmount(),
                bill.getGeneratedAt(),
                bill.getDueDate(),
                paid,
                BillingRules.pending(bill.getAmount(), paid),
                BillingRules.billStatus(bill.getAmount(), paid, bill.getDueDate(), today),
                new CustomerSummary(c.id(), c.accountNo(), c.name(), c.phone(), c.area(), c.stbNo(), c.status(), c.agentId()));
    }

    private BigDecimal paidOn(String billId) {
        BigDecimal sum = payments.sumByBillId(billId);
        return sum == null ? BigDecimal.ZERO : sum;
    }

    private YearMonth resolveCycle(String cycleOrNull) {
        return (cycleOrNull == null || cycleOrNull.isBlank())
                ? YearMonth.now(clock)
                : YearMonth.parse(cycleOrNull.trim()); // yyyy-MM, else DateTimeParseException -> 400
    }

    private static BillStatus parseStatus(String status) {
        return isAll(status) ? null : BillStatus.from(status);
    }

    private static boolean isAll(String value) {
        return value == null || value.isBlank() || "all".equalsIgnoreCase(value.trim());
    }

    private static int count(List<BillResponse> rows, BillStatus status) {
        return (int) rows.stream().filter(b -> b.status() == status).count();
    }

    private static String searchText(BillResponse b, boolean includePhone) {
        CustomerSummary c = b.customer();
        String text = nz(b.billNo()) + " " + nz(c.name()) + " " + nz(c.accountNo()) + " " + nz(c.stbNo());
        if (includePhone) {
            text += " " + nz(c.phone());
        }
        return text.toLowerCase(Locale.ROOT);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
