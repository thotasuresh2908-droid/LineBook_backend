package com.linebook.billing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bills")
public class Bill {

    @Id
    @Column(name = "id", length = 64, nullable = false, updatable = false)
    private String id;

    @Column(name = "bill_no", length = 32, nullable = false, updatable = false)
    private String billNo;

    @Column(name = "customer_id", length = 64, nullable = false, updatable = false)
    private String customerId;

    @Column(name = "cycle", length = 7, nullable = false, updatable = false)
    private String cycle;

    @Column(name = "amount", precision = 12, scale = 2, nullable = false, updatable = false)
    private BigDecimal amount;

    @Column(name = "generated_at", nullable = false, updatable = false)
    private LocalDateTime generatedAt;

    @Column(name = "due_date", nullable = false, updatable = false)
    private LocalDate dueDate;

    protected Bill() {
        // JPA
    }

    public Bill(String id, String billNo, String customerId, String cycle, BigDecimal amount,
                LocalDateTime generatedAt, LocalDate dueDate) {
        this.id = id;
        this.billNo = billNo;
        this.customerId = customerId;
        this.cycle = cycle;
        this.amount = amount;
        this.generatedAt = generatedAt;
        this.dueDate = dueDate;
    }

    public String getId() { return id; }
    public String getBillNo() { return billNo; }
    public String getCustomerId() { return customerId; }
    public String getCycle() { return cycle; }
    public BigDecimal getAmount() { return amount; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public LocalDate getDueDate() { return dueDate; }
}
