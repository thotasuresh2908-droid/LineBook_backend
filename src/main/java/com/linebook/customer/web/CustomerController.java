package com.linebook.customer.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.linebook.customer.dto.AgentAccounts;
import com.linebook.customer.dto.CountsResponse;
import com.linebook.customer.dto.CreateCustomerRequest;
import com.linebook.customer.dto.CustomerResponse;
import com.linebook.customer.dto.UpdateRecurringRequest;
import com.linebook.customer.dto.UpdateStatusRequest;
import com.linebook.customer.service.CustomerService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Customers", description = "Customers: list, add, change status, counts")
@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService service;

    public CustomerController(CustomerService service) {
        this.service = service;
    }

    /**
     * Customers in creation order. {@code ids=a,b,c} restricts to those customers;
     * {@code q}, {@code area} and {@code status} are the Customers page filters; {@code agentId} narrows to one agent's
     * customers (Reports page).
     */
    @Operation(summary = "List customers (Customers page filters)")
    @GetMapping
    public List<CustomerResponse> list(@RequestParam(required = false) List<String> ids,
                                       @RequestParam(required = false) String q,
                                       @RequestParam(required = false) String area,
                                       @RequestParam(required = false) String status,
                                       @RequestParam(required = false) String agentId) {
        return service.list(ids, q, area, status, agentId);
    }

    @Operation(summary = "Customer counts by connection status")
    @GetMapping("/counts")
    public CountsResponse counts() {
        return service.counts();
    }

    @Operation(summary = "List the distinct areas")
    @GetMapping("/areas")
    public List<String> areas() {
        return service.areas();
    }

    /** Connections per agent, for one status (default active). */
    @Operation(summary = "Connections per agent")
    @GetMapping("/agent-counts")
    public List<AgentAccounts> agentCounts(@RequestParam(required = false) String status) {
        return service.agentCounts(status);
    }

    @Operation(summary = "Get one customer")
    @GetMapping("/{id}")
    public CustomerResponse get(@PathVariable String id) {
        return service.get(id);
    }

    @Operation(summary = "Add a customer")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse create(@Valid @RequestBody CreateCustomerRequest request) {
        return service.create(request);
    }

    @Operation(summary = "Change the connection status")
    @PatchMapping("/{id}/status")
    public CustomerResponse setStatus(@PathVariable String id, @Valid @RequestBody UpdateStatusRequest request) {
        return service.setConnectionStatus(id, request.status());
    }

    @Operation(summary = "Turn the monthly bill on or off")
    @PatchMapping("/{id}/recurring")
    public CustomerResponse setRecurring(@PathVariable String id, @Valid @RequestBody UpdateRecurringRequest request) {
        return service.setRecurring(id, request.recurring());
    }
}
