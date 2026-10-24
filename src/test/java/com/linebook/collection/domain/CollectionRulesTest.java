package com.linebook.collection.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CollectionRulesTest {

    @Test
    void amountsAreRoundedToWholeRupees() {
        assertThat(CollectionRules.roundAmount(new BigDecimal("299.4"))).isEqualByComparingTo("299");
        assertThat(CollectionRules.roundAmount(new BigDecimal("299.5"))).isEqualByComparingTo("300");
        assertThat(CollectionRules.roundAmount(new BigDecimal("0.4"))).isEqualByComparingTo("0");
    }

    @Test
    void receiptNumbersFollowTheDayAndAFourDigitSequence() {
        LocalDate day = LocalDate.of(2026, 10, 12);

        assertThat(CollectionRules.receiptPrefix(day)).isEqualTo("RC-20261012-");
        assertThat(CollectionRules.receiptNo(day, 1)).isEqualTo("RC-20261012-0001");
        assertThat(CollectionRules.receiptNo(day, 42)).isEqualTo("RC-20261012-0042");
    }

    @Test
    void agentIsTheOneEnteredThenTheCustomersThenTheDefault() {
        assertThat(CollectionRules.resolveAgent("ag-03", "ag-01", "ag-09")).isEqualTo("ag-03");
        assertThat(CollectionRules.resolveAgent(" ", "ag-01", "ag-09")).isEqualTo("ag-01");
        assertThat(CollectionRules.resolveAgent(null, null, "ag-09")).isEqualTo("ag-09");
        assertThat(CollectionRules.resolveAgent(null, "", "ag-09")).isEqualTo("ag-09");
    }
}
