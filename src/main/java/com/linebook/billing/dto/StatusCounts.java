package com.linebook.billing.dto;

public record StatusCounts(int paid, int partial, int pending, int overdue) {
}
