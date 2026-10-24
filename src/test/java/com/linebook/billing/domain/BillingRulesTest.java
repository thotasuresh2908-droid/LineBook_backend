package com.linebook.billing.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.linebook.customer.domain.ConnectionStatus;
import com.linebook.customer.dto.CustomerResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

class BillingRulesTest {

    private static final BigDecimal AMOUNT = new BigDecimal("299");
    private static final LocalDate DUE = LocalDate.of(2026, 10, 10);

    private static CustomerResponse customer(int billingDay) {
        return new CustomerResponse("cu-01", "SCN-1001", "Lakshmi Devi", "9800000001", "", "Ameerpet", "120000000001",
                "VC00440001", "pkg-family", AMOUNT, ConnectionStatus.ACTIVE, true, billingDay, "ag-01",
                LocalDate.of(2026, 1, 1));
    }

    @Test
    void onlyActiveRecurringCustomersAreBillable() {
        assertThat(BillingRules.isBillable(ConnectionStatus.ACTIVE, true)).isTrue();
        assertThat(BillingRules.isBillable(ConnectionStatus.ACTIVE, false)).isFalse();
        assertThat(BillingRules.isBillable(ConnectionStatus.SUSPENDED, true)).isFalse();
        assertThat(BillingRules.isBillable(ConnectionStatus.PENDING, true)).isFalse();
        assertThat(BillingRules.isBillable(ConnectionStatus.DISCONNECTED, true)).isFalse();
    }

    @Test
    void makeBillBuildsIdNumberAmountAndDates() {
        Bill bill = BillingRules.makeBill(customer(10), YearMonth.of(2026, 10), 7);

        assertThat(bill.getId()).isEqualTo("bl-2026-10-cu-01");
        assertThat(bill.getBillNo()).isEqualTo("BL-202610-0007");
        assertThat(bill.getCycle()).isEqualTo("2026-10");
        assertThat(bill.getAmount()).isEqualByComparingTo("299");
        assertThat(bill.getGeneratedAt()).isEqualTo(LocalDateTime.of(2026, 10, 1, 8, 15, 0));
        assertThat(bill.getDueDate()).isEqualTo(LocalDate.of(2026, 10, 10));
    }

    @Test
    void dueDayIsClampedBetween1And28() {
        assertThat(BillingRules.makeBill(customer(0), YearMonth.of(2026, 10), 1).getDueDate().getDayOfMonth()).isEqualTo(1);
        assertThat(BillingRules.makeBill(customer(31), YearMonth.of(2026, 2), 1).getDueDate().getDayOfMonth()).isEqualTo(28);
    }

    @Test
    void fullyPaidIsPaidEvenWhenLate() {
        assertThat(BillingRules.billStatus(AMOUNT, new BigDecimal("299"), DUE, DUE.plusDays(30))).isEqualTo(BillStatus.PAID);
    }

    @Test
    void within0001OfAmountCountsAsPaid() {
        assertThat(BillingRules.billStatus(AMOUNT, new BigDecimal("298.9995"), DUE, DUE.minusDays(1))).isEqualTo(BillStatus.PAID);
    }

    @Test
    void partialBeforeDueDateIsPartial() {
        assertThat(BillingRules.billStatus(AMOUNT, new BigDecimal("100"), DUE, DUE.minusDays(1))).isEqualTo(BillStatus.PARTIAL);
    }

    @Test
    void partialAfterDueDateIsOverdue() {
        assertThat(BillingRules.billStatus(AMOUNT, new BigDecimal("100"), DUE, DUE.plusDays(1))).isEqualTo(BillStatus.OVERDUE);
    }

    @Test
    void unpaidIsPendingUntilTheDayAfterDueDate() {
        assertThat(BillingRules.billStatus(AMOUNT, BigDecimal.ZERO, DUE, DUE)).isEqualTo(BillStatus.PENDING);
        assertThat(BillingRules.billStatus(AMOUNT, BigDecimal.ZERO, DUE, DUE.plusDays(1))).isEqualTo(BillStatus.OVERDUE);
    }

    @Test
    void pendingIsNeverNegative() {
        assertThat(BillingRules.pending(AMOUNT, new BigDecimal("500"))).isEqualByComparingTo("0");
        assertThat(BillingRules.pending(AMOUNT, new BigDecimal("99"))).isEqualByComparingTo("200");
    }
}
