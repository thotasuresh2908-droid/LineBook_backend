package com.linebook.agent.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.linebook.agent.exception.InvalidAgentException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class AgentRulesTest {

    @Test
    void agentIdsAreTwoDigitsThenGrow() {
        assertThat(AgentRules.agentId(1)).isEqualTo("ag-01");
        assertThat(AgentRules.agentId(12)).isEqualTo("ag-12");
        assertThat(AgentRules.agentId(100)).isEqualTo("ag-100");
    }

    @Test
    void phoneKeepsDigitsOnlyAndAtMostTen() {
        assertThat(AgentRules.normalizePhone("98-490 11201")).isEqualTo("9849011201");
        assertThat(AgentRules.normalizePhone("+91 98490 11201")).isEqualTo("9198490112");
        assertThat(AgentRules.normalizePhone("12345")).isEqualTo("12345");
    }

    @Test
    void aPhoneWithoutDigitsIsRejected() {
        assertThatThrownBy(() -> AgentRules.normalizePhone("n/a")).isInstanceOf(InvalidAgentException.class);
        assertThatThrownBy(() -> AgentRules.normalizePhone(null)).isInstanceOf(InvalidAgentException.class);
    }

    @Test
    void percentCollectedIsAWholeNumberAndZeroWhenNothingIsBilled() {
        assertThat(AgentRules.percentCollected(new BigDecimal("100"), new BigDecimal("299"))).isEqualTo(33);
        assertThat(AgentRules.percentCollected(new BigDecimal("2"), new BigDecimal("3"))).isEqualTo(67);
        assertThat(AgentRules.percentCollected(new BigDecimal("299"), new BigDecimal("299"))).isEqualTo(100);
        assertThat(AgentRules.percentCollected(BigDecimal.ZERO, BigDecimal.ZERO)).isZero();
        assertThat(AgentRules.percentCollected(new BigDecimal("50"), BigDecimal.ZERO)).isZero();
    }
}
