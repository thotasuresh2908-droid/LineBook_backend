package com.linebook.customer.domain;

/**
 * Pure customer rules, a direct port of the LineBook front end (lib/store.ts addCustomer,
 * setConnectionStatus, setRecurring and lib/billing.ts isBillable). No Spring, no I/O.
 */
public final class CustomerRules {

    private CustomerRules() {
    }

    /** accountNo: "SCN-" + (1000 + seq), where seq = number of customers + 1. */
    public static String accountNo(long seq) {
        return "SCN-" + (1000 + seq);
    }

    /** Default set-top-box number when none is entered: String(120000000000 + seq). */
    public static String defaultStbNo(long seq) {
        return String.valueOf(120_000_000_000L + seq);
    }

    /** Default VC number when none is entered: "VC" + padStart(440000 + seq, 8, "0"). */
    public static String defaultVcNo(long seq) {
        return "VC" + String.format("%08d", 440_000L + seq);
    }

    /** Only an active connection can recur (addCustomer / setRecurring). */
    public static boolean effectiveRecurring(ConnectionStatus status, boolean requested) {
        return status == ConnectionStatus.ACTIVE && requested;
    }

    /** Changing the connection status: recurring becomes true when active, false otherwise. */
    public static boolean recurringAfterStatusChange(ConnectionStatus newStatus) {
        return newStatus == ConnectionStatus.ACTIVE;
    }

    /** isBillable: status === "active" && recurring */
    public static boolean isBillable(ConnectionStatus status, boolean recurring) {
        return status == ConnectionStatus.ACTIVE && recurring;
    }
}
