package com.linebook.customer.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CustomerRulesTest {

    @Test
    void accountNumberStartsAt1001() {
        assertThat(CustomerRules.accountNo(1)).isEqualTo("SCN-1001");
        assertThat(CustomerRules.accountNo(37)).isEqualTo("SCN-1037");
    }

    @Test
    void defaultStbAndVcNumbersFollowTheSequence() {
        assertThat(CustomerRules.defaultStbNo(37)).isEqualTo("120000000037");
        assertThat(CustomerRules.defaultVcNo(37)).isEqualTo("VC00440037");
    }

    @Test
    void onlyActiveConnectionsCanRecur() {
        assertThat(CustomerRules.effectiveRecurring(ConnectionStatus.ACTIVE, true)).isTrue();
        assertThat(CustomerRules.effectiveRecurring(ConnectionStatus.ACTIVE, false)).isFalse();
        assertThat(CustomerRules.effectiveRecurring(ConnectionStatus.SUSPENDED, true)).isFalse();
        assertThat(CustomerRules.effectiveRecurring(ConnectionStatus.PENDING, true)).isFalse();
        assertThat(CustomerRules.effectiveRecurring(ConnectionStatus.DISCONNECTED, true)).isFalse();
    }

    @Test
    void statusChangeTurnsRecurringOnForActiveAndOffOtherwise() {
        assertThat(CustomerRules.recurringAfterStatusChange(ConnectionStatus.ACTIVE)).isTrue();
        assertThat(CustomerRules.recurringAfterStatusChange(ConnectionStatus.SUSPENDED)).isFalse();
        assertThat(CustomerRules.recurringAfterStatusChange(ConnectionStatus.PENDING)).isFalse();
        assertThat(CustomerRules.recurringAfterStatusChange(ConnectionStatus.DISCONNECTED)).isFalse();
    }

    @Test
    void billableMeansActiveAndRecurring() {
        assertThat(CustomerRules.isBillable(ConnectionStatus.ACTIVE, true)).isTrue();
        assertThat(CustomerRules.isBillable(ConnectionStatus.ACTIVE, false)).isFalse();
        assertThat(CustomerRules.isBillable(ConnectionStatus.SUSPENDED, true)).isFalse();
    }
}
