package com.linebook.agent.domain;

import com.linebook.agent.exception.InvalidAgentException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pure agent rules. No Spring, no I/O. The percentage is the Agents page's
 * {@code billed ? Math.round(cyclePaid / billed * 100) : 0}.
 */
public final class AgentRules {

    private AgentRules() {
    }

    /** Agent ids look like ag-01, ag-02 ... ag-99, ag-100. */
    public static String agentId(long seq) {
        return String.format("ag-%02d", seq);
    }

    /** Same as customers: digits only, at most 10 of them; nothing left is an error. */
    public static String normalizePhone(String raw) {
        String digits = raw == null ? "" : raw.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            throw new InvalidAgentException("phone must contain digits");
        }
        return digits.length() > 10 ? digits.substring(0, 10) : digits;
    }

    /** Share of the cycle's billed amount that has been collected, rounded to a whole percent; 0 when nothing is billed. */
    public static int percentCollected(BigDecimal collected, BigDecimal billed) {
        if (billed.signum() == 0) {
            return 0;
        }
        return collected.multiply(BigDecimal.valueOf(100)).divide(billed, 0, RoundingMode.HALF_UP).intValue();
    }
}
