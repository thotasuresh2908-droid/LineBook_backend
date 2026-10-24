package com.linebook.agent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.linebook.agent.domain.Agent;
import com.linebook.agent.dto.AgentResponse;
import com.linebook.agent.dto.CreateAgentRequest;
import com.linebook.agent.dto.UpdateAgentRequest;
import com.linebook.agent.exception.AgentNotFoundException;
import com.linebook.agent.exception.InvalidAgentException;
import com.linebook.agent.repository.AgentRepository;
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
class AgentServiceTest {

    @Mock AgentRepository agents;

    AgentService service;

    @BeforeEach
    void setUp() {
        service = new AgentService(agents);
        when(agents.save(any(Agent.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static Agent agent(String id, boolean active) {
        return new Agent(id, "Ramesh Yadav", "9849011201", "Ameerpet", active);
    }

    @Test
    void newAgentsContinueTheNumberingAndStartActive() {
        when(agents.count()).thenReturn(5L);

        AgentResponse created = service.create(new CreateAgentRequest("  Mahesh B ", "98-480 11223", " Uppal "));

        assertThat(created.id()).isEqualTo("ag-06");
        assertThat(created.name()).isEqualTo("Mahesh B");
        assertThat(created.phone()).isEqualTo("9848011223");
        assertThat(created.area()).isEqualTo("Uppal");
        assertThat(created.active()).isTrue();
    }

    @Test
    void aPhoneWithoutDigitsIsRejected() {
        when(agents.count()).thenReturn(5L);

        assertThatThrownBy(() -> service.create(new CreateAgentRequest("Mahesh B", "n/a", "Uppal")))
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
    }
}
