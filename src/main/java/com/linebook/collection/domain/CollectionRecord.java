package com.linebook.collection.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One payment received from a customer against a bill (the front end's Collection), with its receipt details. */
@Entity
@Table(name = "collections")
public class CollectionRecord {

    @Id
    @Column(name = "id", length = 64, nullable = false, updatable = false)
    private String id;

    @Column(name = "receipt_no", length = 32, nullable = false, updatable = false)
    private String receiptNo;

    @Column(name = "bill_id", length = 64, nullable = false, updatable = false)
    private String billId;

    @Column(name = "bill_no", length = 32, nullable = false, updatable = false)
    private String billNo;

    @Column(name = "customer_id", length = 64, nullable = false, updatable = false)
    private String customerId;

    @Column(name = "customer_name", length = 150, nullable = false, updatable = false)
    private String customerName;

    @Column(name = "account_no", length = 32, nullable = false, updatable = false)
    private String accountNo;

    @Column(name = "stb_no", length = 32, nullable = false, updatable = false)
    private String stbNo;

    @Column(name = "agent_id", length = 64, nullable = false, updatable = false)
    private String agentId;

    @Column(name = "agent_name", length = 150, updatable = false)
    private String agentName;

    @Column(name = "amount", precision = 12, scale = 2, nullable = false, updatable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", length = 8, nullable = false, updatable = false)
    private PayMethod method;

    @Column(name = "collected_at", nullable = false, updatable = false)
    private LocalDateTime collectedAt;

    @Column(name = "notes", length = 500, nullable = false, updatable = false)
    private String notes;

    protected CollectionRecord() {
        // JPA
    }

    public CollectionRecord(String id, String receiptNo, String billId, String billNo, String customerId,
                            String customerName, String accountNo, String stbNo, String agentId, String agentName,
                            BigDecimal amount, PayMethod method, LocalDateTime collectedAt, String notes) {
        this.id = id;
        this.receiptNo = receiptNo;
        this.billId = billId;
        this.billNo = billNo;
        this.customerId = customerId;
        this.customerName = customerName;
        this.accountNo = accountNo;
        this.stbNo = stbNo;
        this.agentId = agentId;
        this.agentName = agentName;
        this.amount = amount;
        this.method = method;
        this.collectedAt = collectedAt;
        this.notes = notes;
    }

    public String getId() { return id; }
    public String getReceiptNo() { return receiptNo; }
    public String getBillId() { return billId; }
    public String getBillNo() { return billNo; }
    public String getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
    public String getAccountNo() { return accountNo; }
    public String getStbNo() { return stbNo; }
    public String getAgentId() { return agentId; }
    public String getAgentName() { return agentName; }
    public BigDecimal getAmount() { return amount; }
    public PayMethod getMethod() { return method; }
    public LocalDateTime getCollectedAt() { return collectedAt; }
    public String getNotes() { return notes; }
}
