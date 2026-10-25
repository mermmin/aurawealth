package com.aurawealth.dashboard;

import java.math.BigDecimal;
import java.time.Instant;

/** {@code asOf} is the oldest last-sync across the user's banks, or null before the first bank is linked. */
public record NetWorthResponse(BigDecimal assets, BigDecimal liabilities, BigDecimal netWorth, String currency,
		Instant asOf) {
}
