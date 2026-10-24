package com.linebook.billing.service;

import com.linebook.customer.event.CustomerBillableEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * When a customer becomes billable (added, activated, or recurring switched on), create this month's bill.
 * Runs synchronously inside the customer's transaction: if the bill cannot be created, the customer change is rolled
 * back too, so a billable customer never exists without its bill.
 */
@Component
public class BillingEventListener {

    private final BillingService billing;

    public BillingEventListener(BillingService billing) {
        this.billing = billing;
    }

    @EventListener
    void onCustomerBillable(CustomerBillableEvent event) {
        billing.ensureCurrentCycleBill(event.customerId());
    }
}
