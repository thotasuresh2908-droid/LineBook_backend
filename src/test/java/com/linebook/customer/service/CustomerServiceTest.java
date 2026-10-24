package com.linebook.customer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.linebook.agent.dto.AgentResponse;
import com.linebook.agent.service.AgentService;
import com.linebook.customer.event.CustomerBillableEvent;
import com.linebook.customer.domain.ConnectionStatus;
import com.linebook.customer.domain.Customer;
import com.linebook.customer.domain.PackagePlan;
import com.linebook.customer.dto.CreateCustomerRequest;
import com.linebook.customer.dto.CustomerResponse;
import com.linebook.customer.exception.CustomerNotFoundException;
import com.linebook.customer.exception.InvalidCustomerException;
import com.linebook.customer.dto.AgentAccounts;
import com.linebook.customer.repository.AgentCount;
import com.linebook.customer.repository.CustomerRepository;
import com.linebook.customer.repository.PackagePlanRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CustomerServiceTest {

    // 12 Oct 2026, Asia/Kolkata
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-12T06:00:00Z"), ZoneId.of("Asia/Kolkata"));
    private static final PackagePlan BASIC = new PackagePlan("pkg-basic", "Basic FTA", new BigDecimal("199"), 1);
    private static final PackagePlan FAMILY = new PackagePlan("pkg-family", "Family", new BigDecimal("299"), 2);

    @Mock CustomerRepository customers;
    @Mock PackagePlanRepository packages;
    @Mock AgentService agents;
    @Mock ApplicationEventPublisher events;

    CustomerService service;

    @BeforeEach
    void setUp() {
        service = new CustomerService(customers, packages, agents, events, CLOCK);
        when(agents.find("ag-01")).thenReturn(Optional.of(new AgentResponse("ag-01", "Ramesh Yadav", "9849011201", "Ameerpet", true)));
        when(customers.count()).thenReturn(36L);
        when(packages.findAllByOrderBySortOrderAsc()).thenReturn(List.of(BASIC, FAMILY));
        when(packages.findById("pkg-family")).thenReturn(Optional.of(FAMILY));
        when(packages.findById("pkg-unknown")).thenReturn(Optional.empty());
        when(customers.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static CreateCustomerRequest request(String packageId, ConnectionStatus status, Boolean recurring,
                                                 Boolean generateBill) {
        return new CreateCustomerRequest("  Ravi Kumar ", "98-480 11223 3 99", "  12-3, Lane 4 ", "Ameerpet",
                null, "", packageId, "ag-01", status, recurring, generateBill, null);
    }

    private static Customer customer(ConnectionStatus status, boolean recurring) {
        return new Customer("cu-1", "SCN-1001", "Lakshmi", "9800000001", "", "Ameerpet", "STB1", "VC1",
                "pkg-family", new BigDecimal("299"), status, recurring, 1, "ag-01", LocalDate.of(2026, 1, 1));
    }

    @Test
    void createAppliesTheSameDefaultsAsTheUi() {
        CustomerResponse c = service.create(request("pkg-family", null, null, null));

        assertThat(c.accountNo()).isEqualTo("SCN-1037");
        assertThat(c.name()).isEqualTo("Ravi Kumar");
        assertThat(c.phone()).isEqualTo("9848011223");            // digits only, max 10
        assertThat(c.address()).isEqualTo("12-3, Lane 4");
        assertThat(c.stbNo()).isEqualTo("120000000037");
        assertThat(c.vcNo()).isEqualTo("VC00440037");
        assertThat(c.packageId()).isEqualTo("pkg-family");
        assertThat(c.monthlyRate()).isEqualByComparingTo("299");
        assertThat(c.status()).isEqualTo(ConnectionStatus.ACTIVE);
        assertThat(c.recurring()).isTrue();
        assertThat(c.billingDay()).isEqualTo(1);
        assertThat(c.since()).isEqualTo(LocalDate.of(2026, 10, 12));
        verify(events).publishEvent(any(CustomerBillableEvent.class));
    }

    @Test
    void unknownPackageFallsBackToTheFirstPackage() {
        CustomerResponse c = service.create(request("pkg-unknown", null, null, null));

        assertThat(c.packageId()).isEqualTo("pkg-basic");
        assertThat(c.monthlyRate()).isEqualByComparingTo("199");
    }

    @Test
    void recurringIsForcedOffWhenNotActive() {
        CustomerResponse c = service.create(request("pkg-family", ConnectionStatus.PENDING, true, true));

        assertThat(c.recurring()).isFalse();
        verify(events, never()).publishEvent(any(CustomerBillableEvent.class));
    }

    @Test
    void noBillEventWhenGenerateBillIsOff() {
        service.create(request("pkg-family", ConnectionStatus.ACTIVE, true, false));

        verify(events, never()).publishEvent(any(CustomerBillableEvent.class));
    }

    @Test
    void phoneWithoutDigitsIsRejected() {
        CreateCustomerRequest bad = new CreateCustomerRequest("Ravi", "abc", null, "Ameerpet", null, null,
                "pkg-family", "ag-01", null, null, null, null);

        assertThatThrownBy(() -> service.create(bad)).isInstanceOf(InvalidCustomerException.class);
    }

    @Test
    void activatingTurnsRecurringOnAndAsksForABill() {
        Customer existing = customer(ConnectionStatus.SUSPENDED, false);
        when(customers.findById("cu-1")).thenReturn(Optional.of(existing));

        CustomerResponse c = service.setConnectionStatus("cu-1", ConnectionStatus.ACTIVE);

        assertThat(c.status()).isEqualTo(ConnectionStatus.ACTIVE);
        assertThat(c.recurring()).isTrue();
        verify(events).publishEvent(any(CustomerBillableEvent.class));
    }

    @Test
    void suspendingTurnsRecurringOffAndDoesNotAskForABill() {
        Customer existing = customer(ConnectionStatus.ACTIVE, true);
        when(customers.findById("cu-1")).thenReturn(Optional.of(existing));

        CustomerResponse c = service.setConnectionStatus("cu-1", ConnectionStatus.SUSPENDED);

        assertThat(c.recurring()).isFalse();
        verify(events, never()).publishEvent(any(CustomerBillableEvent.class));
    }

    @Test
    void recurringCannotBeTurnedOnForAnInactiveConnection() {
        Customer existing = customer(ConnectionStatus.SUSPENDED, false);
        when(customers.findById("cu-1")).thenReturn(Optional.of(existing));

        CustomerResponse c = service.setRecurring("cu-1", true);

        assertThat(c.recurring()).isFalse();
        verify(events, never()).publishEvent(any(CustomerBillableEvent.class));
    }

    @Test
    void recurringOnForActiveConnectionAsksForABill() {
        Customer existing = customer(ConnectionStatus.ACTIVE, false);
        when(customers.findById("cu-1")).thenReturn(Optional.of(existing));

        CustomerResponse c = service.setRecurring("cu-1", true);

        assertThat(c.recurring()).isTrue();
        verify(events).publishEvent(any(CustomerBillableEvent.class));
    }

    @Test
    void unknownCustomerIsNotFound() {
        when(customers.findById("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get("nope")).isInstanceOf(CustomerNotFoundException.class);
    }

    @Test
    void anUnknownAgentIsRejectedAndNothingIsSaved() {
        when(agents.find("ag-99")).thenReturn(Optional.empty());
        CreateCustomerRequest bad = new CreateCustomerRequest("Ravi", "9848011223", null, "Ameerpet", null, null,
                "pkg-family", "ag-99", null, null, null, null);

        assertThatThrownBy(() -> service.create(bad)).isInstanceOf(InvalidCustomerException.class);
        verify(customers, never()).save(any(Customer.class));
    }

    @Test
    void activeAccountsAreCountedPerAgent() {
        when(customers.countByAgent(ConnectionStatus.ACTIVE))
                .thenReturn(List.of(new AgentCount("ag-01", 12L), new AgentCount("ag-02", 3L)));

        assertThat(service.activeAccountsByAgent()).containsEntry("ag-01", 12L).containsEntry("ag-02", 3L);
    }

    @Test
    void agentCountsDefaultToActiveConnections() {
        when(customers.countByAgent(ConnectionStatus.ACTIVE)).thenReturn(List.of(new AgentCount("ag-01", 12L)));

        assertThat(service.agentCounts(null)).containsExactly(new AgentAccounts("ag-01", 12));
        assertThat(service.agentCounts("all")).containsExactly(new AgentAccounts("ag-01", 12));
    }

    @Test
    void otherModulesCanLookACustomerUpWithoutAnException() {
        Customer existing = customer(ConnectionStatus.ACTIVE, true);
        when(customers.findById("cu-1")).thenReturn(Optional.of(existing));
        when(customers.findById("nope")).thenReturn(Optional.empty());

        assertThat(service.find("cu-1")).get().extracting(CustomerResponse::accountNo).isEqualTo("SCN-1001");
        assertThat(service.find("nope")).isEmpty();
    }
}
