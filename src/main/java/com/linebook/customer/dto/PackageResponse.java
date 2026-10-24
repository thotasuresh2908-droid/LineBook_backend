package com.linebook.customer.dto;

import com.linebook.customer.domain.PackagePlan;
import java.math.BigDecimal;

public record PackageResponse(String id, String name, BigDecimal amount) {

    public static PackageResponse from(PackagePlan p) {
        return new PackageResponse(p.getId(), p.getName(), p.getAmount());
    }
}
