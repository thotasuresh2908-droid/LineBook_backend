package com.linebook.collection.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Test
    void withoutACycleTheListDefaultsToToday() {
        LocalDate today = LocalDate.of(2026, 10, 12);

        CollectionRules.Window none = CollectionRules.window(null, null, false, today);
        assertThat(none.from()).isEqualTo(LocalDateTime.of(2026, 10, 12, 0, 0));
        assertThat(none.toExclusive()).isEqualTo(LocalDateTime.of(2026, 10, 13, 0, 0));

        CollectionRules.Window fromOnly = CollectionRules.window(LocalDate.of(2026, 10, 5), null, false, today);
        assertThat(fromOnly.from()).isEqualTo(LocalDateTime.of(2026, 10, 5, 0, 0));
        assertThat(fromOnly.toExclusive()).isEqualTo(LocalDateTime.of(2026, 10, 6, 0, 0));      // same day

        CollectionRules.Window range = CollectionRules.window(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 9), false, today);
        assertThat(range.toExclusive()).isEqualTo(LocalDateTime.of(2026, 10, 10, 0, 0));        // "to" is inclusive
    }

    @Test
    void withACycleAMissingEndStaysOpen() {
        LocalDate today = LocalDate.of(2026, 10, 12);

        CollectionRules.Window open = CollectionRules.window(null, null, true, today);
        assertThat(open.from()).isNull();
        assertThat(open.toExclusive()).isNull();

        CollectionRules.Window half = CollectionRules.window(null, LocalDate.of(2026, 10, 20), true, today);
        assertThat(half.from()).isNull();
        assertThat(half.toExclusive()).isEqualTo(LocalDateTime.of(2026, 10, 21, 0, 0));
    }
}
