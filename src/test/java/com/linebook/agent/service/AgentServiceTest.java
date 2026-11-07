package com.linebook.agent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.linebook.agent.domain.Agent;
import com.linebook.agent.dto.AgentResponse;
import com.linebook.agent.dto.CreateAgentRequest;
import com.linebook.agent.dto.UpdateAgentRequest;
import com.linebook.agent.exception.AgentNotFoundException;
import com.linebook.agent.exception.InvalidAgentException;
import com.linebook.agent.repository.AgentRepository;
import com.linebook.auth.service.AuthService;
import com.linebook.collection.repository.CollectionRepository;
import com.linebook.customer.repository.CustomerRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgentServiceTest {

    @Mock AgentRepository agents;
    @Mock CustomerRepository customers;
    @Mock CollectionRepository collections;
    @Mock AuthService auth;
    @Mock ObjectProvider<AuthService> authProvider;

    AgentService service;

    @BeforeEach
    void setUp() {
        when(authProvider.getObject()).thenReturn(auth);
        service = new AgentService(agents, customers, collections, authProvider);
        when(agents.save(any(Agent.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static Agent agent(String id, boolean active) {
        return new Agent(id, "Ramesh Yadav", "9849011201", "Ameerpet", active);
    }

    /** The three-field form: an agent with no login, exactly as before. */
    private static CreateAgentRequest plain(String name, String phone, String area) {
        return new CreateAgentRequest(name, phone, area, null, null);
    }

    @Test
    void newAgentsContinueTheNumberingAndStartActive() {
        when(agents.maxSeq()).thenReturn(5L);

        AgentResponse created = service.create(plain("  Mahesh B ", "98-480 11223", " Uppal "));

        assertThat(created.id()).isEqualTo("ag-06");
        assertThat(created.name()).isEqualTo("Mahesh B");
        assertThat(created.phone()).isEqualTo("9848011223");
        assertThat(created.area()).isEqualTo("Uppal");
        assertThat(created.active()).isTrue();
    }

    @Test
    void anAgentWithoutLoginDetailsGetsNoLogin() {
        when(agents.maxSeq()).thenReturn(0L);

        service.create(plain("Mahesh B", "9848011223", "Uppal"));

        verify(auth, never()).createForAgent(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void anEmailAndPasswordCreateTheAgentsLoginToo() {
        when(agents.maxSeq()).thenReturn(0L);

        service.create(new CreateAgentRequest("Mahesh B", "9848011223", "Uppal",
                "mahesh@example.com", "secret123"));

        verify(auth).createForAgent(eq("mahesh@example.com"), eq("Mahesh B"), eq("secret123"), eq("ag-01"));
    }

    @Test
    void halfTheLoginDetailsIsRejected() {
        when(agents.maxSeq()).thenReturn(0L);

        assertThatThrownBy(() -> service.create(
                new CreateAgentRequest("Mahesh B", "9848011223", "Uppal", "mahesh@example.com", null)))
                .isInstanceOf(InvalidAgentException.class);
        assertThatThrownBy(() -> service.create(
                new CreateAgentRequest("Mahesh B", "9848011223", "Uppal", null, "secret123")))
                .isInstanceOf(InvalidAgentException.class);
    }

    @Test
    void aPhoneWithoutDigitsIsRejected() {
        when(agents.maxSeq()).thenReturn(5L);

        assertThatThrownBy(() -> service.create(plain("Mahesh B", "n/a", "Uppal")))
                .isInstanceOf(InvalidAgentException.class);
    }

    @Test
    void updateChangesNamePhoneAndArea() {
        when(agents.findById("ag-01")).thenReturn(Optional.of(agent("ag-01", true)));

        AgentResponse updated = service.update("ag-01", new UpdateAgentRequest("Ramesh Y", "9000000000", "Uppal"));

        assertThat(updated.name()).isEqualTo("Ramesh Y");
        assertThat(updated.phone()).isEqualTo("9000000000");
        assertThat(updated.area()).isEqualTo("Uppal");
        assertThat(updated.id()).isEqualTo("ag-01");
    }

    @Test
    void deactivatingAndReactivating() {
        when(agents.findById("ag-01")).thenReturn(Optional.of(agent("ag-01", true)));

        assertThat(service.setActive("ag-01", false).active()).isFalse();
        assertThat(service.setActive("ag-01", true).active()).isTrue();
    }

    @Test
    void listIsActiveOnlyUnlessInactiveAreAsked() {
        when(agents.findByActiveTrueOrderBySeqAsc()).thenReturn(List.of(agent("ag-01", true)));
        when(agents.findAllByOrderBySeqAsc()).thenReturn(List.of(agent("ag-01", true), agent("ag-02", false)));

        assertThat(service.list(false)).extracting(AgentResponse::id).containsExactly("ag-01");
        assertThat(service.list(true)).extracting(AgentResponse::id).containsExactly("ag-01", "ag-02");
        verify(agents).findByActiveTrueOrderBySeqAsc();
    }

    @Test
    void anInactiveAgentCanStillBeLookedUp() {
        when(agents.findById("ag-02")).thenReturn(Optional.of(agent("ag-02", false)));

        assertThat(service.get("ag-02").active()).isFalse();
    }

    @Test
    void unknownAgentIsNotFound() {
        when(agents.findById("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get("nope")).isInstanceOf(AgentNotFoundException.class);
        assertThatThrownBy(() -> service.setActive("nope", false)).isInstanceOf(AgentNotFoundException.class);
        assertThatThrownBy(() -> service.delete("nope")).isInstanceOf(AgentNotFoundException.class);
    }

    // ------------------------------------------------------------------ delete

    @Test
    void anAgentWithNothingInTheBooksIsDeletedWithTheirLogin() {
        when(agents.findById("ag-02")).thenReturn(Optional.of(agent("ag-02", true)));
        when(customers.countByAgentId("ag-02")).thenReturn(0L);
        when(collections.countByAgentId("ag-02")).thenReturn(0L);

        service.delete("ag-02");

        verify(auth).deleteForAgent("ag-02");
        verify(agents).delete(any(Agent.class));
    }

    @Test
    void anAgentWithCustomersIsNotDeleted() {
        when(agents.findById("ag-02")).thenReturn(Optional.of(agent("ag-02", true)));
        when(customers.countByAgentId("ag-02")).thenReturn(7L);

        assertThatThrownBy(() -> service.delete("ag-02"))
                .isInstanceOf(InvalidAgentException.class)
                .hasMessageContaining("7");
        verify(agents, never()).delete(any(Agent.class));
        verify(auth, never()).deleteForAgent(anyString());
    }

    @Test
    void anAgentWithReceiptsIsNotDeletedSoTheHistoryStaysIntact() {
        when(agents.findById("ag-02")).thenReturn(Optional.of(agent("ag-02", true)));
        when(customers.countByAgentId("ag-02")).thenReturn(0L);
        when(collections.countByAgentId("ag-02")).thenReturn(3L);

        assertThatThrownBy(() -> service.delete("ag-02"))
                .isInstanceOf(InvalidAgentException.class)
                .hasMessageContaining("Deactivate");
        verify(agents, never()).delete(any(Agent.class));
    }
}
