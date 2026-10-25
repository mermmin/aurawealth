package com.aurawealth.transaction;

import java.math.BigDecimal;

/** Money spent in one Plaid category (personal_finance_category.primary); category is null for uncategorized. */
public record CategorySpending(String category, BigDecimal amount) {
}
