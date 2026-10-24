package com.linebook.customer.event;

/**
 * Published, inside the customer's transaction, when a customer is billable (active and recurring) after being added,
 * activated or having recurring switched on. The billing module reacts by creating this month's bill, so the customer
 * change and the bill are saved together or not at all.
 */
public record CustomerBillableEvent(String customerId) {
}
