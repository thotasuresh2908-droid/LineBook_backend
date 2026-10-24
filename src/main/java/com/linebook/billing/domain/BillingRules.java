package com.linebook.billing.domain;

import com.linebook.customer.domain.ConnectionStatus;
import com.linebook.customer.dto.CustomerResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Pure billing rules, a direct port of the LineBook front-end logic
 * (lib/billing.ts and makeBill in lib/store.ts). No Spring, no I/O.
 */
public final class BillingRules {

    /** Same 0.001 tolerance the UI uses when deciding a bill is fully paid. */
    private static final BigDecimal TOLERANCE = new BigDecimal("0.001");

    private BillingRules() {
    }

    /** isBillable: status === "active" && recurring */
    public static boolean isBillable(ConnectionStatus status, boolean recurring) {
        return status == ConnectionStatus.ACTIVE && recurring;
    }

    public static BigDecimal pending(BigDecimal amount, BigDecimal paid) {
        return amount.subtract(paid).max(BigDecimal.ZERO);
    }

    /**
     * billStatus: paid when paid >= amount - 0.001; otherwise overdue once today is after
     * the due date (even if partly paid); otherwise partial (some paid) or pending.
     */
    public static BillStatus billStatus(BigDecimal amount, BigDecimal paid, LocalDate dueDate, LocalDate today) {
        if (paid.compareTo(amount.subtract(TOLERANCE)) >= 0) {
            return BillStatus.PAID;
        }
        boolean overdue = today.isAfter(dueDate);
        if (paid.signum() > 0) {
            return overdue ? BillStatus.OVERDUE : BillStatus.PARTIAL;
        }
        return overdue ? BillStatus.OVERDUE : BillStatus.PENDING;
    }

    /**
     * makeBill: id "bl-{cycle}-{customerId}", number "BL-{yyyyMM}-{seq:04}", amount = monthly rate,
     * generated on the 1st at 08:15, due on min(28, max(1, billingDay)) of the cycle month.
     */
    public static Bill makeBill(CustomerResponse customer, YearMonth cycle, int seq) {
        int dueDay = Math.min(28, Math.max(1, customer.billingDay()));
        String cycleKey = cycle.toString(); // yyyy-MM
        return new Bill(
                "bl-" + cycleKey + "-" + customer.id(),
                "BL-" + cycleKey.replace("-", "") + "-" + String.format("%04d", seq),
                customer.id(),
                cycleKey,
                customer.monthlyRate(),
                cycle.atDay(1).atTime(8, 15, 0),
                cycle.atDay(dueDay));
    }
}
