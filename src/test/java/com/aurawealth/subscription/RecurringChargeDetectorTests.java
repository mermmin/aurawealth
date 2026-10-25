package com.aurawealth.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.aurawealth.subscription.RecurringChargeDetector.Charge;
import com.aurawealth.subscription.RecurringChargeDetector.Detected;

class RecurringChargeDetectorTests {

	private static final LocalDate AUG_9 = LocalDate.of(2026, 8, 9);

	private final RecurringChargeDetector detector = new RecurringChargeDetector();

	@Test
	void detectsMonthlyChargeDescribedByItsLatestOccurrence() {
		List<Detected> detected = detector.detect(List.of(
				charge("Touchstone Climbing", "78.50", AUG_9),
				charge("Touchstone Climbing", "78.50", LocalDate.of(2026, 9, 8)),
				charge("Touchstone Climbing", "78.50", LocalDate.of(2026, 10, 9))));

		assertThat(detected).singleElement().satisfies((subscription) -> {
			assertThat(subscription.merchantKey()).isEqualTo("touchstone climbing");
			assertThat(subscription.merchantName()).isEqualTo("Touchstone Climbing");
			assertThat(subscription.frequency()).isEqualTo(Frequency.MONTHLY);
			assertThat(subscription.amount()).isEqualByComparingTo("78.50");
			assertThat(subscription.lastChargedOn()).isEqualTo(LocalDate.of(2026, 10, 9));
			assertThat(subscription.category()).isEqualTo("PERSONAL_CARE");
		});
	}

	@Test
	void detectsWeeklyAndAnnualCharges() {
		List<Charge> charges = new ArrayList<>();
		for (int week = 0; week < 4; week++) {
			charges.add(charge("Meal Kit", "60.00", AUG_9.plusWeeks(week).plusDays(week % 2)));
		}
		charges.add(charge("Domain Renewal", "12.00", LocalDate.of(2024, 10, 1)));
		charges.add(charge("Domain Renewal", "12.00", LocalDate.of(2025, 10, 3)));

		assertThat(detector.detect(charges)).extracting(Detected::merchantName, Detected::frequency)
			.containsExactly(tuple("Domain Renewal", Frequency.ANNUAL),
					tuple("Meal Kit", Frequency.WEEKLY));
	}

	@Test
	void irregularGapsAreNotASubscription() {
		// Real sandbox pattern: two Uber rides a month, 13 then 17 days apart
		List<Charge> rides = List.of(charge("Uber", "5.40", LocalDate.of(2026, 8, 12)),
				charge("Uber", "6.33", LocalDate.of(2026, 8, 25)), charge("Uber", "5.40", LocalDate.of(2026, 9, 11)),
				charge("Uber", "6.33", LocalDate.of(2026, 9, 24)));

		assertThat(detector.detect(rides)).isEmpty();
	}

	@Test
	void oneOddGapBreaksThePattern() {
		assertThat(detector.detect(List.of(charge("Gym", "30", AUG_9), charge("Gym", "30", AUG_9.plusMonths(1)),
				charge("Gym", "30", AUG_9.plusMonths(3))))).isEmpty();
	}

	@Test
	void twoMonthlyChargesAreACoincidenceNotASubscription() {
		assertThat(detector.detect(List.of(charge("Gym", "30", AUG_9), charge("Gym", "30", AUG_9.plusMonths(1)))))
			.isEmpty();
	}

	@Test
	void priceIncreaseWithinToleranceStillMatchesAndReportsNewPrice() {
		List<Detected> detected = detector.detect(List.of(charge("Netflix", "15.49", AUG_9),
				charge("Netflix", "15.49", AUG_9.plusMonths(1)), charge("Netflix", "17.99", AUG_9.plusMonths(2))));

		assertThat(detected).singleElement().extracting(Detected::amount).isEqualTo(new BigDecimal("17.99"));
	}

	@Test
	void amountsVaryingTooMuchAreNotASubscription() {
		// Same store on the same day each month, but a different basket each time
		assertThat(detector.detect(List.of(charge("Grocer", "40", AUG_9), charge("Grocer", "95", AUG_9.plusMonths(1)),
				charge("Grocer", "62", AUG_9.plusMonths(2))))).isEmpty();
	}

	@Test
	void merchantNamesGroupIgnoringCaseAndSpacesAndRefundsAreIgnored() {
		List<Detected> detected = detector.detect(List.of(charge("Spotify", "11.99", AUG_9),
				charge("SPOTIFY ", "11.99", AUG_9.plusMonths(1)), charge(" spotify", "-11.99", AUG_9.plusDays(3)),
				charge("spotify", "11.99", AUG_9.plusMonths(2))));

		assertThat(detected).singleElement().satisfies((subscription) -> {
			assertThat(subscription.merchantKey()).isEqualTo("spotify");
			assertThat(subscription.merchantName()).isEqualTo("spotify");
		});
	}

	@Test
	void missedAnnualRenewalTurnsInactiveAfterTheGracePeriodNotAYearLater() {
		LocalDate today = LocalDate.of(2026, 10, 25);
		Detected lapsed = new Detected("calm", "Calm", new BigDecimal("69.99"), "USD", Frequency.ANNUAL,
				LocalDate.of(2025, 1, 10), null);
		Detected renewsSoon = new Detected("domain", "Domain", BigDecimal.TEN, "USD", Frequency.ANNUAL,
				LocalDate.of(2025, 10, 1), null);

		assertThat(lapsed.isActiveOn(today)).isFalse();
		assertThat(renewsSoon.isActiveOn(today)).isTrue();
		assertThat(renewsSoon.isActiveOn(LocalDate.of(2026, 10, 31))).isTrue();
		assertThat(renewsSoon.isActiveOn(LocalDate.of(2026, 11, 1))).isFalse();
	}

	@Test
	void monthlyStaysActiveUntilTheNextChargeIs30DaysOverdue() {
		Detected monthly = new Detected("gym", "Gym", BigDecimal.TEN, "USD", Frequency.MONTHLY,
				LocalDate.of(2026, 8, 20), null);

		assertThat(monthly.isActiveOn(LocalDate.of(2026, 9, 25))).isTrue();
		assertThat(monthly.isActiveOn(LocalDate.of(2026, 10, 20))).isTrue();
		assertThat(monthly.isActiveOn(LocalDate.of(2026, 10, 21))).isFalse();
	}

	@Test
	void monthlyCostPutsFrequenciesOnOneScale() {
		assertThat(Frequency.WEEKLY.monthlyCost(new BigDecimal("12.00"))).isEqualByComparingTo("52.00");
		assertThat(Frequency.MONTHLY.monthlyCost(new BigDecimal("78.50"))).isEqualByComparingTo("78.50");
		assertThat(Frequency.ANNUAL.monthlyCost(new BigDecimal("119.88"))).isEqualByComparingTo("9.99");
	}

	private static Charge charge(String merchant, String amount, LocalDate date) {
		return new Charge(merchant, new BigDecimal(amount), "USD", date, "PERSONAL_CARE");
	}

}
