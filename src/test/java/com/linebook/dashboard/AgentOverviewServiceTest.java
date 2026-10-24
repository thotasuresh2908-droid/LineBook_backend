package com.linebook.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.linebook.agent.dto.AgentResponse;
import com.linebook.agent.service.AgentService;
import com.linebook.billing.dto.AgentSummary;
import com.linebook.billing.service.BillingService;
import com.linebook.collection.dto.AgentTotal;
import com.linebook.collection.dto.DaySummary;
import com.linebook.collection.service.CollectionQueryService;
import com.linebook.customer.service.CustomerService;
import com.linebook.dashboard.dto.AgentOverview;
import com.linebook.dashboard.service.AgentOverviewService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgentOverviewServiceTest {

    @Mock AgentService agents;
    @Mock CustomerService customers;
    @Mock BillingService billing;
    @Mock CollectionQueryService collections;

    AgentOverviewService service;

    @BeforeEach
    void setUp() {
        service = new AgentOverviewService(agents, customers, billing, collections);
        when(agents.list(false)).thenReturn(List.of(
                new AgentResponse("ag-01", "Ramesh Yadav", "9849011201", "Ameerpet", true),
                new AgentResponse("ag-02", "S. Padma", "9866123344", "Kukatpally", true)));
        when(customers.activeAccountsByAgent()).thenReturn(Map.of("ag-01", 12L));
        when(billing.summaryByAgent(null)).thenReturn(List.of(new AgentSummary(
                "ag-01", 10, new BigDecimal("2990"), new BigDecimal("1000"), new BigDecimal("1990"))));
        when(collections.summary(null)).thenReturn(new DaySummary(LocalDate.of(2026, 10, 12), 2, new BigDecimal("598"),
                List.of(new AgentTotal("ag-01", new BigDecimal("598"), 2))));
    }

    @Test
    void combinesTheModulesIntoOneCardPerAgent() {
        AgentOverview ramesh = service.overview(null, null, false).get(0);

        assertThat(ramesh.id()).isEqualTo("ag-01");
        assertThat(ramesh.name()).isEqualTo("Ramesh Yadav");
        assertThat(ramesh.activeAccounts()).isEqualTo(12);
        assertThat(ramesh.todayAmount()).isEqualByComparingTo("598");
        assertThat(ramesh.todayCount()).isEqualTo(2);
        assertThat(ramesh.cycleBilled()).isEqualByComparingTo("2990");
        assertThat(ramesh.cycleCollected()).isEqualByComparingTo("1000");
        assertThat(ramesh.cyclePending()).isEqualByComparingTo("1990");
        assertThat(ramesh.cyclePercent()).isEqualTo(33);
    }

    @Test
    void anAgentWithNoActivityGetsZeros() {
        AgentOverview padma = service.overview(null, null, false).get(1);

        assertThat(padma.activeAccounts()).isZero();
        assertThat(padma.todayAmount()).isEqualByComparingTo("0");
        assertThat(padma.todayCount()).isZero();
        assertThat(padma.cycleBilled()).isEqualByComparingTo("0");
        assertThat(padma.cyclePercent()).isZero();
    }

    @Test
    void noAgentsGivesAnEmptyList() {
        when(agents.list(false)).thenReturn(List.of());

        assertThat(service.overview(null, null, false)).isEmpty();
    }
}
