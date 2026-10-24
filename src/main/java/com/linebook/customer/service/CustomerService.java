package com.linebook.customer.service;

import com.linebook.agent.service.AgentService;
import com.linebook.customer.domain.ConnectionStatus;
import com.linebook.customer.domain.Customer;
import com.linebook.customer.domain.CustomerRules;
import com.linebook.customer.domain.PackagePlan;
import com.linebook.customer.dto.AgentAccounts;
import com.linebook.customer.dto.CountsResponse;
import com.linebook.customer.dto.CreateCustomerRequest;
import com.linebook.customer.dto.CustomerResponse;
import com.linebook.customer.dto.PackageResponse;
import com.linebook.customer.event.CustomerBillableEvent;
import com.linebook.customer.exception.CustomerNotFoundException;
import com.linebook.customer.exception.InvalidCustomerException;
import com.linebook.customer.repository.AgentCount;
import com.linebook.customer.repository.CustomerRepository;
import com.linebook.customer.repository.CustomerSpecs;
import com.linebook.customer.repository.PackagePlanRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Customer use cases. Behaviour matches the LineBook front-end store:
 * addCustomer, setConnectionStatus, setRecurring and the Customers page filters.
 * Other modules (billing, collections) read customers through the lookup methods at the bottom.
 */
@Service
public class CustomerService {

    private static final int ID_CHUNK = 500;

    private final CustomerRepository customers;
    private final PackagePlanRepository packages;
    private final AgentService agents;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public CustomerService(CustomerRepository customers, PackagePlanRepository packages, AgentService agents,
                           ApplicationEventPublisher events, Clock clock) {
        this.customers = customers;
        this.packages = packages;
        this.agents = agents;
        this.events = events;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ commands

    /** "Add customer". Publishes a billable event when the new connection should get this month's bill. */
    @Transactional
    public CustomerResponse create(CreateCustomerRequest request) {
        String phone = normalizePhone(request.phone());
        String agentId = request.agentId().trim();
        if (agents.find(agentId).isEmpty()) {
            throw new InvalidCustomerException("Unknown agent: " + agentId);
        }
        PackagePlan pkg = request.packageId() == null
                ? firstPackage()
                : packages.findById(request.packageId()).orElseGet(this::firstPackage);

        long seq = customers.count() + 1;
        ConnectionStatus status = request.status() == null ? ConnectionStatus.ACTIVE : request.status();
        boolean requestedRecurring = request.recurring() == null || request.recurring();
        boolean recurring = CustomerRules.effectiveRecurring(status, requestedRecurring);
        boolean generateBill = request.generateBill() == null || request.generateBill();
        int billingDay = request.billingDay() == null ? 1 : request.billingDay();

        Customer customer = customers.save(new Customer(
                "cu-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12),
                CustomerRules.accountNo(seq),
                request.name().trim(),
                phone,
                request.address() == null ? "" : request.address().trim(),
                request.area().trim(),
                blank(request.stbNo()) ? CustomerRules.defaultStbNo(seq) : request.stbNo().trim(),
                blank(request.vcNo()) ? CustomerRules.defaultVcNo(seq) : request.vcNo().trim(),
                pkg.getId(),
                pkg.getAmount(),
                status,
                recurring,
                billingDay,
                agentId,
                LocalDate.now(clock)));

        if (generateBill) {
            publishIfBillable(customer);
        }
        return CustomerResponse.from(customer);
    }

    /** Change the connection status. Active turns recurring on; any other status turns it off. */
    @Transactional
    public CustomerResponse setConnectionStatus(String id, ConnectionStatus status) {
        Customer customer = findEntity(id);
        customer.changeStatus(status, CustomerRules.recurringAfterStatusChange(status));
        customers.save(customer);
        publishIfBillable(customer);
        return CustomerResponse.from(customer);
    }

    /** Toggle the monthly bill. Ignored (stays off) unless the connection is active. */
    @Transactional
    public CustomerResponse setRecurring(String id, boolean recurring) {
        Customer customer = findEntity(id);
        customer.changeRecurring(CustomerRules.effectiveRecurring(customer.getStatus(), recurring));
        customers.save(customer);
        publishIfBillable(customer);
        return CustomerResponse.from(customer);
    }

    // ------------------------------------------------------------------ queries (Customers page)

    @Transactional(readOnly = true)
    public CustomerResponse get(String id) {
        return CustomerResponse.from(findEntity(id));
    }

    /** Customers in creation order. All filters optional; "all" (or blank) means no filter. */
    @Transactional(readOnly = true)
    public List<CustomerResponse> list(Collection<String> ids, String q, String area, String status) {
        Specification<Customer> spec = (root, query, cb) -> cb.conjunction();
        if (ids != null && !ids.isEmpty()) {
            spec = spec.and(CustomerSpecs.idIn(ids));
        }
        if (!isAll(area)) {
            spec = spec.and(CustomerSpecs.inArea(area.trim()));
        }
        if (!isAll(status)) {
            spec = spec.and(CustomerSpecs.hasStatus(ConnectionStatus.from(status)));
        }
        if (q != null && !q.isBlank()) {
            spec = spec.and(CustomerSpecs.matches(q));
        }
        return customers.findAll(spec, Sort.by("seq")).stream().map(CustomerResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CountsResponse counts() {
        return new CountsResponse(
                customers.count(),
                customers.countByStatus(ConnectionStatus.ACTIVE),
                customers.countByStatus(ConnectionStatus.PENDING),
                customers.countByStatus(ConnectionStatus.SUSPENDED),
                customers.countByStatus(ConnectionStatus.DISCONNECTED),
                customers.countByStatusAndRecurring(ConnectionStatus.ACTIVE, true));
    }

    @Transactional(readOnly = true)
    public List<String> areas() {
        return customers.findDistinctAreas();
    }

    @Transactional(readOnly = true)
    public List<PackageResponse> packages() {
        return packages.findAllByOrderBySortOrderAsc().stream().map(PackageResponse::from).toList();
    }

    /** Connections per agent for one status (default active). */
    @Transactional(readOnly = true)
    public List<AgentAccounts> agentCounts(String statusOrNull) {
        ConnectionStatus status = isAll(statusOrNull) ? ConnectionStatus.ACTIVE : ConnectionStatus.from(statusOrNull);
        return customers.countByAgent(status).stream()
                .map(c -> new AgentAccounts(c.agentId(), c.count()))
                .toList();
    }

    // ------------------------------------------------------------------ lookups for other modules

    /** Active connections per agent id (the "active accounts" on the Agents page). */
    @Transactional(readOnly = true)
    public Map<String, Long> activeAccountsByAgent() {
        Map<String, Long> result = new HashMap<>();
        for (AgentCount c : customers.countByAgent(ConnectionStatus.ACTIVE)) {
            result.merge(c.agentId(), c.count(), Long::sum);
        }
        return result;
    }

    /** Every customer in creation order. */
    @Transactional(readOnly = true)
    public List<CustomerResponse> findAll() {
        return customers.findAll(Sort.by("seq")).stream().map(CustomerResponse::from).toList();
    }

    /** One customer, or empty when unknown. */
    @Transactional(readOnly = true)
    public Optional<CustomerResponse> find(String id) {
        return customers.findById(id).map(CustomerResponse::from);
    }

    /** The customers with these ids (unknown ids are skipped), looked up in chunks. */
    @Transactional(readOnly = true)
    public List<CustomerResponse> findByIds(Collection<String> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        List<String> all = new ArrayList<>(ids);
        List<CustomerResponse> result = new ArrayList<>();
        for (int i = 0; i < all.size(); i += ID_CHUNK) {
            result.addAll(list(all.subList(i, Math.min(i + ID_CHUNK, all.size())), null, null, null));
        }
        return result;
    }

    // ------------------------------------------------------------------ helpers

    private Customer findEntity(String id) {
        return customers.findById(id).orElseThrow(() -> new CustomerNotFoundException(id));
    }

    private PackagePlan firstPackage() {
        return packages.findAllByOrderBySortOrderAsc().stream().findFirst()
                .orElseThrow(() -> new InvalidCustomerException("No packages are configured"));
    }

    private void publishIfBillable(Customer customer) {
        if (CustomerRules.isBillable(customer.getStatus(), customer.isRecurring())) {
            events.publishEvent(new CustomerBillableEvent(customer.getId()));
        }
    }

    /** The UI keeps digits only and at most 10 of them. */
    private static String normalizePhone(String raw) {
        String digits = raw.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            throw new InvalidCustomerException("phone must contain digits");
        }
        return digits.length() > 10 ? digits.substring(0, 10) : digits;
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static boolean isAll(String value) {
        return value == null || value.isBlank() || "all".equalsIgnoreCase(value.trim());
    }
}
