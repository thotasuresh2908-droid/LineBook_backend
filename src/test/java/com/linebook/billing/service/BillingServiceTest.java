package com.linebook.billing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.linebook.billing.domain.Bill;
import com.linebook.billing.domain.BillPayment;
import com.linebook.billing.domain.BillStatus;
import com.linebook.billing.domain.BillingRules;
import com.linebook.billing.dto.AgentSummary;
import com.linebook.billing.dto.BillResponse;
import com.linebook.billing.dto.GenerateResponse;
import com.linebook.billing.exception.InvalidPaymentException;
import com.linebook.billing.repository.BillPaymentRepository;
import com.linebook.billing.repository.BillRepository;
import com.linebook.billing.repository.PaidTotal;
import com.linebook.customer.domain.ConnectionStatus;
import com.linebook.customer.dto.CustomerResponse;
import com.linebook.customer.service.CustomerService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BillingServiceTest {

    // 12 Oct 2026, Asia/Kolkata
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-12T06:00:00Z"), ZoneId.of("Asia/Kolkata"));

    @Mock BillRepository bills;
    @Mock BillPaymentRepository payments;
    @Mock CustomerService customers;
    @Captor ArgumentCaptor<List<Bill>> savedBills;

    BillingService service;

    @BeforeEach
    void setUp() {
        service = new BillingService(bills, payments, customers, CLOCK);
    }

    private static CustomerResponse customer(String id, ConnectionStatus status, boolean recurring, int billingDay) {
        return customer(id, status, recurring, billingDay, "ag-01");
    }

    private static CustomerResponse customer(String id, ConnectionStatus status, boolean recurring, int billingDay,
                                             String agentId) {
        return new CustomerResponse(id, "SCN-" + id, "Name " + id, "9800000000", "", "Ameerpet", "STB" + id,
                "VC" + id, "pkg-family", new BigDecimal("299"), status, recurring, billingDay, agentId,
                LocalDate.of(2026, 1, 1));
    }

    @Test
    void generateCycleBillsOnlyBillableCustomersWithoutABillAndNumbersAfterExistingOnes() {
        when(bills.findCustomerIdsByCycle("2026-10")).thenReturn(Set.of("c-3"));
        when(customers.findAll()).thenReturn(List.of(
                customer("c-1", ConnectionStatus.ACTIVE, true, 1),
                customer("c-2", ConnectionStatus.ACTIVE, false, 1),      // recurring off
                customer("c-3", ConnectionStatus.ACTIVE, true, 1),       // already billed
                customer("c-4", ConnectionStatus.SUSPENDED, true, 1),    // not active
                customer("c-5", ConnectionStatus.ACTIVE, true, 1)));

        GenerateResponse result = service.generateCycle("2026-10");

        assertThat(result.created()).isEqualTo(2);
        assertThat(result.cycle()).isEqualTo("2026-10");
        verify(bills).saveAll(savedBills.capture());
        assertThat(savedBills.getValue()).extracting(Bill::getId)
                .containsExactly("bl-2026-10-c-1", "bl-2026-10-c-5");
        assertThat(savedBills.getValue()).extracting(Bill::getBillNo)
                .containsExactly("BL-202610-0002", "BL-202610-0003");
    }

    @Test
    void generateCycleDefaultsToCurrentCycleAndIsIdempotent() {
        when(bills.findCustomerIdsByCycle("2026-10")).thenReturn(Set.of("c-1"));
        when(customers.findAll()).thenReturn(List.of(customer("c-1", ConnectionStatus.ACTIVE, true, 1)));

        GenerateResponse result = service.generateCycle(null);

        assertThat(result.created()).isZero();
        assertThat(result.cycle()).isEqualTo("2026-10");
        verify(bills, never()).saveAll(any());
    }

    @Test
    void paymentAboveThePendingBalanceIsRejected() {
        CustomerResponse c = customer("c-1", ConnectionStatus.ACTIVE, true, 28);
        Bill bill = BillingRules.makeBill(c, YearMonth.of(2026, 10), 1);
        when(bills.findByIdForUpdate(bill.getId())).thenReturn(Optional.of(bill));
        when(customers.find("c-1")).thenReturn(Optional.of(c));
        when(payments.existsByCollectionId("co-1")).thenReturn(false);
        when(payments.sumByBillId(bill.getId())).thenReturn(null);

        assertThatThrownBy(() -> service.recordPayment(bill.getId(), "co-1", new BigDecimal("300")))
                .isInstanceOf(InvalidPaymentException.class);
        assertThatThrownBy(() -> service.recordPayment(bill.getId(), "co-1", BigDecimal.ZERO))
                .isInstanceOf(InvalidPaymentException.class);
        verify(payments, never()).save(any());
    }

    @Test
    void partialPaymentIsRecordedAndBillBecomesPartial() {
        CustomerResponse c = customer("c-1", ConnectionStatus.ACTIVE, true, 28); // due 28 Oct, so not overdue yet
        Bill bill = BillingRules.makeBill(c, YearMonth.of(2026, 10), 1);
        when(bills.findByIdForUpdate(bill.getId())).thenReturn(Optional.of(bill));
        when(customers.find("c-1")).thenReturn(Optional.of(c));
        when(payments.existsByCollectionId("co-1")).thenReturn(false);
        when(payments.sumByBillId(bill.getId())).thenReturn((BigDecimal) null, new BigDecimal("100"));

        BillResponse response = service.recordPayment(bill.getId(), "co-1", new BigDecimal("100"));

        verify(payments).save(any(BillPayment.class));
        assertThat(response.paid()).isEqualByComparingTo("100");
        assertThat(response.pending()).isEqualByComparingTo("199");
        assertThat(response.status()).isEqualTo(BillStatus.PARTIAL);
    }

    @Test
    void customerBillsAcrossCyclesAreReturnedNewestFirstWithTheirStatus() {
        CustomerResponse c = customer("c-1", ConnectionStatus.ACTIVE, true, 10);
        Bill september = BillingRules.makeBill(c, YearMonth.of(2026, 9), 1);
        Bill october = BillingRules.makeBill(c, YearMonth.of(2026, 10), 1);
        when(customers.find("c-1")).thenReturn(Optional.of(c));
        when(bills.findByCustomerIdOrderByCycleDesc("c-1")).thenReturn(List.of(october, september));
        when(payments.sumByBillId(september.getId())).thenReturn(new BigDecimal("299"));
        when(payments.sumByBillId(october.getId())).thenReturn(null);

        List<BillResponse> result = service.listForCustomer("c-1");

        assertThat(result).extracting(BillResponse::cycle).containsExactly("2026-10", "2026-09");
        assertThat(result).extracting(BillResponse::status)
                .containsExactly(BillStatus.OVERDUE, BillStatus.PAID);   // 12 Oct is after the 10 Oct due date
    }

    @Test
    void summaryByAgentGroupsTheCycleBillsOfEachAgentsCustomers() {
        CustomerResponse a1 = customer("c-1", ConnectionStatus.ACTIVE, true, 28, "ag-01");
        CustomerResponse a2 = customer("c-2", ConnectionStatus.ACTIVE, true, 28, "ag-01");
        CustomerResponse b1 = customer("c-3", ConnectionStatus.ACTIVE, true, 28, "ag-02");
        Bill bill1 = BillingRules.makeBill(a1, YearMonth.of(2026, 10), 1);
        Bill bill2 = BillingRules.makeBill(a2, YearMonth.of(2026, 10), 2);
        Bill bill3 = BillingRules.makeBill(b1, YearMonth.of(2026, 10), 3);
        when(bills.findByCycleOrderByBillNoAsc("2026-10")).thenReturn(List.of(bill1, bill2, bill3));
        when(payments.sumByCycle("2026-10")).thenReturn(List.of(
                new PaidTotal(bill1.getId(), new BigDecimal("100")),
                new PaidTotal(bill3.getId(), new BigDecimal("299"))));
        when(customers.findByIds(any())).thenReturn(List.of(a1, a2, b1));

        List<AgentSummary> result = service.summaryByAgent(null);

        assertThat(result).extracting(AgentSummary::agentId).containsExactly("ag-01", "ag-02");
        AgentSummary first = result.get(0);
        assertThat(first.billCount()).isEqualTo(2);
        assertThat(first.billed()).isEqualByComparingTo("598");
        assertThat(first.collected()).isEqualByComparingTo("100");
        assertThat(first.pending()).isEqualByComparingTo("498");
        AgentSummary second = result.get(1);
        assertThat(second.billed()).isEqualByComparingTo("299");
        assertThat(second.collected()).isEqualByComparingTo("299");
        assertThat(second.pending()).isEqualByComparingTo("0");
    }

    @Test
    void replayingACollectionIdDoesNotCountTwice() {
        CustomerResponse c = customer("c-1", ConnectionStatus.ACTIVE, true, 28);
        Bill bill = BillingRules.makeBill(c, YearMonth.of(2026, 10), 1);
        when(bills.findByIdForUpdate(bill.getId())).thenReturn(Optional.of(bill));
        when(customers.find("c-1")).thenReturn(Optional.of(c));
        when(payments.existsByCollectionId("co-1")).thenReturn(true);
        when(payments.sumByBillId(bill.getId())).thenReturn(new BigDecimal("100"));

        BillResponse response = service.recordPayment(bill.getId(), "co-1", new BigDecimal("100"));

        verify(payments, never()).save(any());
        assertThat(response.paid()).isEqualByComparingTo("100");
    }
}
