package com.aurawealth.subscription;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Finds recurring charges in a transaction history. Plain Java with no Spring or database, so the rules are easy to
 * read and test. A merchant is recurring when:
 * <ul>
 * <li>every gap between consecutive charges fits one {@link Frequency} (so irregular spending never matches),</li>
 * <li>there are at least {@link Frequency#minCharges()} charges, and</li>
 * <li>every amount is within {@link #AMOUNT_TOLERANCE} of the typical (median) amount, which allows price changes
 * like a streaming service going from $15.49 to $17.99 (+16%).</li>
 * </ul>
 */
public class RecurringChargeDetector {

	static final BigDecimal AMOUNT_TOLERANCE = new BigDecimal("0.20");

	/** One money-out transaction. {@code merchant} is Plaid's merchant name, or the transaction name without one. */
	public record Charge(String merchant, BigDecimal amount, String isoCurrencyCode, LocalDate date, String category) {
	}

	/** A recurring charge, described by its latest occurrence. */
	public record Detected(String merchantKey, String merchantName, BigDecimal amount, String isoCurrencyCode,
			Frequency frequency, LocalDate lastChargedOn, String category) {

		/** Still active until it has missed a whole expected charge, i.e. two periods have passed since the last. */
		public boolean isActiveOn(LocalDate today) {
			return !today.isAfter(lastChargedOn.plus(frequency.period()).plus(frequency.period()));
		}

	}

	public List<Detected> detect(List<Charge> charges) {
		Map<String, List<Charge>> byMerchant = charges.stream()
			.filter((charge) -> charge.amount().signum() > 0)
			.filter((charge) -> charge.merchant() != null && !charge.merchant().isBlank())
			.collect(Collectors.groupingBy((charge) -> merchantKey(charge.merchant())));
		return byMerchant.entrySet()
			.stream()
			.map((merchant) -> detect(merchant.getKey(), merchant.getValue()))
			.flatMap(Optional::stream)
			.sorted(Comparator.comparing(Detected::merchantKey))
			.toList();
	}

	/** "NETFLIX " and "Netflix" are the same merchant. */
	static String merchantKey(String merchant) {
		return merchant.trim().toLowerCase(Locale.ROOT);
	}

	private static Optional<Detected> detect(String merchantKey, List<Charge> charges) {
		List<Charge> byDate = charges.stream().sorted(Comparator.comparing(Charge::date)).toList();
		Optional<Frequency> frequency = frequencyOf(byDate);
		if (frequency.isEmpty() || !amountsAreSimilar(byDate)) {
			return Optional.empty();
		}
		Charge latest = byDate.get(byDate.size() - 1);
		return Optional.of(new Detected(merchantKey, latest.merchant().trim(), latest.amount(),
				latest.isoCurrencyCode(), frequency.get(), latest.date(), latest.category()));
	}

	private static Optional<Frequency> frequencyOf(List<Charge> byDate) {
		List<Long> gaps = new ArrayList<>();
		for (int i = 1; i < byDate.size(); i++) {
			gaps.add(ChronoUnit.DAYS.between(byDate.get(i - 1).date(), byDate.get(i).date()));
		}
		return Arrays.stream(Frequency.values())
			.filter((frequency) -> byDate.size() >= frequency.minCharges())
			.filter((frequency) -> gaps.stream().allMatch(frequency::matchesGap))
			.findFirst();
	}

	private static boolean amountsAreSimilar(List<Charge> charges) {
		List<BigDecimal> amounts = charges.stream().map(Charge::amount).sorted().toList();
		BigDecimal median = amounts.get(amounts.size() / 2);
		BigDecimal allowed = median.multiply(AMOUNT_TOLERANCE);
		return amounts.stream().allMatch((amount) -> amount.subtract(median).abs().compareTo(allowed) <= 0);
	}

}
