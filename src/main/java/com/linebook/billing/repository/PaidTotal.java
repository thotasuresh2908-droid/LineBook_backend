package com.linebook.billing.repository;

import java.math.BigDecimal;

public record PaidTotal(String billId, BigDecimal total) {
}
