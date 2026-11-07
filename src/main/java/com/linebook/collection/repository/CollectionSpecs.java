package com.linebook.collection.repository;

import com.linebook.collection.domain.CollectionRecord;
import java.time.LocalDateTime;
import org.springframework.data.jpa.domain.Specification;

public final class CollectionSpecs {

    private CollectionSpecs() {
    }

    /** collectedAt in [from, to). */
    public static Specification<CollectionRecord> collectedBetween(LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> cb.and(
                cb.greaterThanOrEqualTo(root.<LocalDateTime>get("collectedAt"), from),
                cb.lessThan(root.<LocalDateTime>get("collectedAt"), to));
    }

    /** collectedAt >= from. */
    public static Specification<CollectionRecord> collectedFrom(LocalDateTime from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.<LocalDateTime>get("collectedAt"), from);
    }

    /** collectedAt < toExclusive. */
    public static Specification<CollectionRecord> collectedBefore(LocalDateTime toExclusive) {
        return (root, query, cb) -> cb.lessThan(root.<LocalDateTime>get("collectedAt"), toExclusive);
    }

    /**
     * A billing cycle's collections: those that paid a bill of the cycle, plus any collected during the cycle's month
     * (the Reports page's definition of "this cycle").
     */
    public static Specification<CollectionRecord> inCycle(String cycle, LocalDateTime monthStart, LocalDateTime nextMonthStart) {
        return (root, query, cb) -> cb.or(
                cb.equal(root.get("billCycle"), cycle),
                cb.and(
                        cb.greaterThanOrEqualTo(root.<LocalDateTime>get("collectedAt"), monthStart),
                        cb.lessThan(root.<LocalDateTime>get("collectedAt"), nextMonthStart)));
    }

    public static Specification<CollectionRecord> byAgent(String agentId) {
        return (root, query, cb) -> cb.equal(root.get("agentId"), agentId);
    }

    public static Specification<CollectionRecord> byBill(String billId) {
        return (root, query, cb) -> cb.equal(root.get("billId"), billId);
    }

    public static Specification<CollectionRecord> byCustomer(String customerId) {
        return (root, query, cb) -> cb.equal(root.get("customerId"), customerId);
    }
}
