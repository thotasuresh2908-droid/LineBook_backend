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
