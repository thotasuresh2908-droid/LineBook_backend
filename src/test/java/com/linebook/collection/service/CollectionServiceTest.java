package com.linebook.collection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.linebook.agent.dto.AgentResponse;
import com.linebook.agent.service.AgentService;
import com.linebook.billing.domain.BillStatus;
import com.linebook.billing.dto.BillResponse;
import com.linebook.billing.dto.CustomerSummary;
import com.linebook.billing.exception.InvalidPaymentException;
import com.linebook.billing.service.BillingService;
import com.linebook.collection.domain.CollectionRecord;
import com.linebook.collection.domain.PayMethod;
import com.linebook.collection.dto.CollectRequest;
import com.linebook.collection.dto.CollectResult;
import com.linebook.collection.exception.InvalidCollectionException;
import com.linebook.collection.repository.CollectionRepository;
import com.linebook.common.config.LinebookProperties;
import com.linebook.customer.domain.ConnectionStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CollectionServiceTest {

    // 12 Oct 2026, Asia/Kolkata
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-12T06:00:00Z"), ZoneId.of("Asia/Kolkata"));
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 12);
    private static final LinebookProperties PROPS = props(true);

    @Mock CollectionRepository collections;
    @Mock BillingService billing;
    @Mock AgentService agents;
    @Mock ReceiptNumbers receipts;

    CollectionService service;

    private static LinebookProperties props(boolean currentCycleOnly) {
        return new LinebookProperties("Asia/Kolkata", "Surya Cable Network", "ag-01", currentCycleOnly,
                new LinebookProperties.Cors(List.of("http://localhost:8080"), Duration.ofHours(1)));
    }

    @BeforeEach
    void setUp() {
        service = new CollectionService(collections, billing, agents, receipts, PROPS, CLOCK);
        when(collections.findById(any())).thenReturn(Optional.empty());
        when(collections.save(any(CollectionRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        when(receipts.next(TODAY)).thenReturn(1L);
        when(agents.find("ag-02")).thenReturn(Optional.of(new AgentResponse("ag-02", "S. Padma", "9866123344", "Kukatpally", true)));
        when(agents.find("ag-05")).thenReturn(Optional.of(new AgentResponse("ag-05", "Kiran Kumar", "9123456709", "Secunderabad", true)));
        when(agents.find("ag-01")).thenReturn(Optional.of(new AgentResponse("ag-01", "Ramesh Yadav", "9849011201", "Ameerpet", true)));
    }

    private static BillResponse bill(String cycle, String pending, ConnectionStatus customerStatus) {
        return new BillResponse("bl-1", "BL-202610-0001", "cu-01", cycle, new BigDecimal("299"),
                LocalDateTime.of(2026, 10, 1, 8, 15), LocalDate.of(2026, 10, 28), BigDecimal.ZERO, new BigDecimal(pending),
                BillStatus.PENDING,
                new CustomerSummary("cu-01", "SCN-1001", "Lakshmi Devi", "9800000001", "Ameerpet", "120000000001",
                        customerStatus, "ag-02"));
    }

    private static CollectRequest request(String amount, String agentId, String collectionId) {
        return new CollectRequest("bl-1", new BigDecimal(amount), null, agentId, " paid at door ", collectionId);
    }

    @Test
    void recordsACollectionAppliesItToTheBillAndIssuesAReceipt() {
        when(billing.get("bl-1")).thenReturn(bill("2026-10", "299", ConnectionStatus.ACTIVE));

        CollectResult result = service.collect(request("299", null, null));

        assertThat(result.created()).isTrue();
        assertThat(result.collection().receiptNo()).isEqualTo("RC-20261012-0001");
        assertThat(result.collection().method()).isEqualTo(PayMethod.CASH);          // default
        assertThat(result.collection().agentId()).isEqualTo("ag-02");                // customer's agent
        assertThat(result.collection().agentName()).isEqualTo("S. Padma");           // looked up from the agent module
        assertThat(result.collection().notes()).isEqualTo("paid at door");
        assertThat(result.collection().customerName()).isEqualTo("Lakshmi Devi");
        verify(billing).recordPayment(eq("bl-1"), any(), eq(new BigDecimal("299")));
    }

    @Test
    void anAgentEnteredOnTheRequestWins() {
        when(billing.get("bl-1")).thenReturn(bill("2026-10", "299", ConnectionStatus.ACTIVE));

        CollectResult result = service.collect(request("100", "ag-05", null));

        assertThat(result.collection().agentId()).isEqualTo("ag-05");
        assertThat(result.collection().agentName()).isEqualTo("Kiran Kumar");
    }

    @Test
    void amountsAreRoundedToWholeRupees() {
        when(billing.get("bl-1")).thenReturn(bill("2026-10", "299", ConnectionStatus.ACTIVE));

        service.collect(request("149.6", null, null));

        verify(billing).recordPayment(eq("bl-1"), any(), eq(new BigDecimal("150")));
    }

    @Test
    void anAmountAboveThePendingBalanceOrZeroIsRejectedAndNothingIsSaved() {
        when(billing.get("bl-1")).thenReturn(bill("2026-10", "199", ConnectionStatus.ACTIVE));

        assertThatThrownBy(() -> service.collect(request("200", null, null)))
                .isInstanceOf(InvalidCollectionException.class);
        assertThatThrownBy(() -> service.collect(request("0", null, null)))
                .isInstanceOf(InvalidCollectionException.class);
        verify(billing, never()).recordPayment(any(), any(), any());
        verify(collections, never()).save(any(CollectionRecord.class));
    }

    @Test
    void aSettledBillCannotBeCollected() {
        when(billing.get("bl-1")).thenReturn(bill("2026-10", "0", ConnectionStatus.ACTIVE));

        assertThatThrownBy(() -> service.collect(request("100", null, null)))
                .isInstanceOf(InvalidCollectionException.class);
    }

    @Test
    void aDisconnectedConnectionCannotBeCollected() {
        when(billing.get("bl-1")).thenReturn(bill("2026-10", "299", ConnectionStatus.DISCONNECTED));

        assertThatThrownBy(() -> service.collect(request("100", null, null)))
                .isInstanceOf(InvalidCollectionException.class);
        verify(billing, never()).recordPayment(any(), any(), any());
    }

    @Test
    void onlyCurrentCycleBillsAreCollectedByDefault() {
        when(billing.get("bl-1")).thenReturn(bill("2026-09", "299", ConnectionStatus.ACTIVE));

        assertThatThrownBy(() -> service.collect(request("100", null, null)))
                .isInstanceOf(InvalidCollectionException.class);
        verify(billing, never()).recordPayment(any(), any(), any());
    }

    @Test
    void olderCyclesAreAcceptedWhenCurrentCycleOnlyIsOff() {
        CollectionService relaxed = new CollectionService(collections, billing, agents, receipts, props(false), CLOCK);
        when(billing.get("bl-1")).thenReturn(bill("2026-09", "299", ConnectionStatus.ACTIVE));

        assertThat(relaxed.collect(request("100", null, null)).created()).isTrue();
    }

    @Test
    void anUnknownAgentIsRejectedBeforeThePaymentIsApplied() {
        when(billing.get("bl-1")).thenReturn(bill("2026-10", "299", ConnectionStatus.ACTIVE));
        when(agents.find("ag-99")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.collect(request("100", "ag-99", null)))
                .isInstanceOf(InvalidCollectionException.class);
        verify(billing, never()).recordPayment(any(), any(), any());
    }

    @Test
    void ifBillingRejectsThePaymentNoReceiptNumberIsUsedAndNothingIsSaved() {
        when(billing.get("bl-1")).thenReturn(bill("2026-10", "299", ConnectionStatus.ACTIVE));
        when(billing.recordPayment(eq("bl-1"), any(), any())).thenThrow(new InvalidPaymentException("already paid"));

        assertThatThrownBy(() -> service.collect(request("100", null, null)))
                .isInstanceOf(InvalidPaymentException.class);
        verify(receipts, never()).next(any());
        verify(collections, never()).save(any(CollectionRecord.class));
    }

    @Test
    void replayingACollectionIdReturnsTheExistingCollectionAndTouchesNothing() {
        CollectionRecord existing = new CollectionRecord("co-1", "RC-20261012-0007", "bl-1", "BL-202610-0001", "cu-01",
                "Lakshmi Devi", "SCN-1001", "120000000001", "ag-02", "S. Padma", new BigDecimal("100"), PayMethod.UPI,
                LocalDateTime.of(2026, 10, 12, 9, 0), "");
        when(collections.findById("co-1")).thenReturn(Optional.of(existing));

        CollectResult result = service.collect(request("100", null, "co-1"));

        assertThat(result.created()).isFalse();
        assertThat(result.collection().receiptNo()).isEqualTo("RC-20261012-0007");
        verify(billing, never()).get(any());
        verify(billing, never()).recordPayment(any(), any(), any());
        verify(collections, never()).save(any(CollectionRecord.class));
    }
}
