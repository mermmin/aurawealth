package com.aurawealth.subscription;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aurawealth.subscription.RecurringChargeDetector.Charge;
import com.aurawealth.subscription.RecurringChargeDetector.Detected;
import com.aurawealth.transaction.SpendingCategories;
import com.aurawealth.transaction.TransactionRepository;
import com.aurawealth.user.User;
import com.aurawealth.user.UserRepository;

@Service
public class SubscriptionService {

	private final SubscriptionRepository subscriptionRepository;

	private final TransactionRepository transactionRepository;

	private final UserRepository userRepository;

	private final Clock clock;

	private final RecurringChargeDetector detector = new RecurringChargeDetector();

	public SubscriptionService(SubscriptionRepository subscriptionRepository,
			TransactionRepository transactionRepository, UserRepository userRepository, Clock clock) {
		this.subscriptionRepository = subscriptionRepository;
		this.transactionRepository = transactionRepository;
		this.userRepository = userRepository;
		this.clock = clock;
	}

	/**
	 * Re-runs detection over the user's whole transaction history and brings the ledger in line: new recurring
	 * charges are added, known ones updated, and ones no longer detected are deactivated (kept for history).
	 */
	@Transactional
	public void refresh(Long userId) {
		LocalDate today = LocalDate.now(clock);
		List<Charge> charges = transactionRepository.findByUserIdAndAmountGreaterThan(userId, BigDecimal.ZERO)
			.stream()
			// A monthly loan payment or paycheck transfer is recurring, but it isn't a subscription
			// (null check first: Set.of(...).contains(null) throws)
			.filter((transaction) -> transaction.getCategory() == null
					|| !SpendingCategories.NON_SPENDING.contains(transaction.getCategory()))
			.map((transaction) -> new Charge(
					Objects.requireNonNullElse(transaction.getMerchantName(), transaction.getName()),
					transaction.getAmount(), transaction.getIsoCurrencyCode(), transaction.getDate(),
					transaction.getCategory()))
			.toList();

		Map<String, Subscription> undetected = subscriptionRepository.findByUserId(userId)
			.stream()
			.collect(Collectors.toMap(Subscription::getMerchantKey, Function.identity()));
		User user = userRepository.getReferenceById(userId);
		for (Detected detected : detector.detect(charges)) {
			Subscription existing = undetected.remove(detected.merchantKey());
			if (existing != null) {
				existing.update(detected.merchantName(), detected.amount(), detected.isoCurrencyCode(),
						detected.frequency(), detected.lastChargedOn(), detected.category(), detected.isActiveOn(today));
			}
			else {
				subscriptionRepository.save(new Subscription(user, detected.merchantKey(), detected.merchantName(),
						detected.amount(), detected.isoCurrencyCode(), detected.frequency(), detected.lastChargedOn(),
						detected.category(), detected.isActiveOn(today)));
			}
		}
		undetected.values().forEach(Subscription::deactivate);
	}

	/** Active subscriptions first, then by next expected charge (soonest first), as in the ledger design. */
	@Transactional(readOnly = true)
	public List<SubscriptionResponse> listForUser(Long userId) {
		return subscriptionRepository.findByUserId(userId)
			.stream()
			.map(SubscriptionResponse::from)
			.sorted(Comparator.comparing(SubscriptionResponse::active, Comparator.reverseOrder())
				.thenComparing(SubscriptionResponse::nextExpectedOn)
				.thenComparing(SubscriptionResponse::merchantName))
			.toList();
	}

}
