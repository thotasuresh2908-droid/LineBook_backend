package com.linebook.customer.web;

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

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService service;

    public CustomerController(CustomerService service) {
        this.service = service;
    }

    /**
     * Customers in creation order. {@code ids=a,b,c} restricts to those customers;
     * {@code q}, {@code area} and {@code status} are the Customers page filters.
     */
    @GetMapping
    public List<CustomerResponse> list(@RequestParam(required = false) List<String> ids,
                                       @RequestParam(required = false) String q,
                                       @RequestParam(required = false) String area,
                                       @RequestParam(required = false) String status) {
        return service.list(ids, q, area, status);
    }

    @GetMapping("/counts")
    public CountsResponse counts() {
        return service.counts();
    }

    @GetMapping("/areas")
    public List<String> areas() {
        return service.areas();
    }

    /** Connections per agent, for one status (default active). */
    @GetMapping("/agent-counts")
    public List<AgentAccounts> agentCounts(@RequestParam(required = false) String status) {
        return service.agentCounts(status);
    }

    @GetMapping("/{id}")
    public CustomerResponse get(@PathVariable String id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse create(@Valid @RequestBody CreateCustomerRequest request) {
        return service.create(request);
    }

    @PatchMapping("/{id}/status")
    public CustomerResponse setStatus(@PathVariable String id, @Valid @RequestBody UpdateStatusRequest request) {
        return service.setConnectionStatus(id, request.status());
    }

    @PatchMapping("/{id}/recurring")
    public CustomerResponse setRecurring(@PathVariable String id, @Valid @RequestBody UpdateRecurringRequest request) {
        return service.setRecurring(id, request.recurring());
    }
}
