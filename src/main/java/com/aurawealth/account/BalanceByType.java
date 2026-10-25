package com.aurawealth.account;

import java.math.BigDecimal;

/** Sum of current balances for one Plaid account type; {@code total} is null if every balance of that type is. */
public record BalanceByType(String type, BigDecimal total) {
}
