package com.linebook.billing.web;

import com.linebook.billing.dto.AgentSummary;
import com.linebook.billing.dto.BillResponse;
import com.linebook.billing.dto.GenerateResponse;
import com.linebook.billing.dto.SummaryResponse;
import com.linebook.billing.service.BillingService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bills")
public class BillController {

    private final BillingService service;

    public BillController(BillingService service) {
        this.service = service;
    }

    /** Bills page. All filters optional; cycle defaults to the current month (yyyy-MM). */
    @GetMapping
    public List<BillResponse> list(@RequestParam(required = false) String cycle,
                                   @RequestParam(required = false) String status,
                                   @RequestParam(required = false) String agentId,
                                   @RequestParam(required = false) String q) {
        return service.list(cycle, status, agentId, q);
    }

    /** Cycles that have bills, newest first (the cycle dropdown). */
    @GetMapping("/cycles")
    public List<String> cycles() {
        return service.cycles();
    }

    /** Dashboard totals for a cycle (defaults to the current one). */
    @GetMapping("/summary")
    public SummaryResponse summary(@RequestParam(required = false) String cycle) {
        return service.summary(cycle);
    }

    /** The same totals per agent (the agent's customers' bills in the cycle). */
    @GetMapping("/summary/by-agent")
    public List<AgentSummary> summaryByAgent(@RequestParam(required = false) String cycle) {
        return service.summaryByAgent(cycle);
    }

    /** Bill picker for "Record collection". */
    @GetMapping("/collectable")
    public List<BillResponse> collectable(@RequestParam(required = false) String q) {
        return service.collectable(q);
    }

    /** Customer detail dialog: a customer's bills across all cycles, newest first. */
    @GetMapping("/by-customer/{customerId}")
    public List<BillResponse> forCustomer(@PathVariable String customerId) {
        return service.listForCustomer(customerId);
    }

    @GetMapping("/{id}")
    public BillResponse get(@PathVariable String id) {
        return service.get(id);
    }

    /** "Run monthly billing". */
    @PostMapping("/generate")
    public GenerateResponse generate(@RequestParam(required = false) String cycle) {
        return service.generateCycle(cycle);
    }
}
