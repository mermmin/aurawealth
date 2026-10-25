package com.aurawealth.subscription;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Period;

/**
 * How often a subscription charges. The day ranges are how far apart two consecutive charges may be and still count
 * as this frequency: months run 28-31 days and banks post a day or two late, so the ranges are deliberately loose.
 */
public enum Frequency {

	WEEKLY(Period.ofWeeks(1), 5, 9, 3, 52),

	MONTHLY(Period.ofMonths(1), 26, 35, 3, 12),

	// Two charges are enough: three would need two years of history, more than Plaid usually returns
	ANNUAL(Period.ofYears(1), 350, 380, 2, 1);

	private static final BigDecimal MONTHS_PER_YEAR = BigDecimal.valueOf(12);

	private final Period period;

	private final int minGapDays;

	private final int maxGapDays;

	private final int minCharges;

	private final int chargesPerYear;

	Frequency(Period period, int minGapDays, int maxGapDays, int minCharges, int chargesPerYear) {
		this.period = period;
		this.minGapDays = minGapDays;
		this.maxGapDays = maxGapDays;
		this.minCharges = minCharges;
		this.chargesPerYear = chargesPerYear;
	}

	public Period period() {
		return period;
	}

	public boolean matchesGap(long gapDays) {
		return gapDays >= minGapDays && gapDays <= maxGapDays;
	}

	/** Fewest charges that make a pattern; fewer is a coincidence, not a subscription. */
	public int minCharges() {
		return minCharges;
	}

	/** What one charge of {@code amount} costs per month on average, so the ledger can compare frequencies. */
	public BigDecimal monthlyCost(BigDecimal amount) {
		return amount.multiply(BigDecimal.valueOf(chargesPerYear)).divide(MONTHS_PER_YEAR, 2, RoundingMode.HALF_EVEN);
	}

}
