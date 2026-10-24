package com.linebook.collection.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyTotal(LocalDate date, BigDecimal amount) {
}
