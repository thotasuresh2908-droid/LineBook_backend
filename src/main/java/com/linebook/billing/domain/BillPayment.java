package com.linebook.billing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** An amount applied to a bill. Created when collection-service records a collection. */
@Entity
@Table(name = "bill_payments")
public class BillPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bill_id", length = 64, nullable = false, updatable = false)
    private String billId;

    @Column(name = "collection_id", length = 64, nullable = false, updatable = false)
    private String collectionId;

    @Column(name = "amount", precision = 12, scale = 2, nullable = false, updatable = false)
    private BigDecimal amount;

    @Column(name = "paid_at", nullable = false, updatable = false)
    private LocalDateTime paidAt;

    protected BillPayment() {
        // JPA
    }

    public BillPayment(String billId, String collectionId, BigDecimal amount, LocalDateTime paidAt) {
        this.billId = billId;
        this.collectionId = collectionId;
        this.amount = amount;
        this.paidAt = paidAt;
    }

    public Long getId() { return id; }
    public String getBillId() { return billId; }
    public String getCollectionId() { return collectionId; }
    public BigDecimal getAmount() { return amount; }
    public LocalDateTime getPaidAt() { return paidAt; }
}
