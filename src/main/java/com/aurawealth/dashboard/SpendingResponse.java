package com.aurawealth.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.aurawealth.transaction.CategorySpending;

/** Spending between {@code from} and {@code to} inclusive; categories are sorted biggest first. */
public record SpendingResponse(LocalDate from, LocalDate to, BigDecimal total, String currency,
		List<CategorySpending> categories) {
}
