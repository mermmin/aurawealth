package com.aurawealth.dashboard;

import java.math.BigDecimal;

public record NetWorthResponse(BigDecimal assets, BigDecimal liabilities, BigDecimal netWorth, String currency) {
}
