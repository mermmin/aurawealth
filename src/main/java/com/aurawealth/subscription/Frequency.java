package com.aurawealth.subscription;

import java.time.Period;

/**
 * How often a subscription charges. The day ranges are how far apart two consecutive charges may be and still count
 * as this frequency: months run 28-31 days and banks post a day or two late, so the ranges are deliberately loose.
 */
public enum Frequency {

	WEEKLY(Period.ofWeeks(1), 5, 9),

	MONTHLY(Period.ofMonths(1), 26, 35),

	ANNUAL(Period.ofYears(1), 350, 380);

	private final Period period;

	private final int minGapDays;

	private final int maxGapDays;

	Frequency(Period period, int minGapDays, int maxGapDays) {
		this.period = period;
		this.minGapDays = minGapDays;
		this.maxGapDays = maxGapDays;
	}

	public Period period() {
		return period;
	}

	public boolean matchesGap(long gapDays) {
		return gapDays >= minGapDays && gapDays <= maxGapDays;
	}

}
