package com.linebook.customer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "package_plans")
public class PackagePlan {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected PackagePlan() {
        // JPA
    }

    public PackagePlan(String id, String name, BigDecimal amount, int sortOrder) {
        this.id = id;
        this.name = name;
        this.amount = amount;
        this.sortOrder = sortOrder;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getAmount() { return amount; }
    public int getSortOrder() { return sortOrder; }
}
