package com.aurawealth.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.aurawealth.transaction.Transaction;
import com.aurawealth.transaction.TransactionRepository;
import com.aurawealth.user.User;
import com.aurawealth.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTests {

	private static final Clock OCT_25 = Clock.fixed(Instant.parse("2026-10-25T12:00:00Z"), ZoneOffset.UTC);

	@Mock
	private SubscriptionRepository subscriptionRepository;

	@Mock
	private TransactionRepository transactionRepository;

	@Mock
	private UserRepository userRepository;

	@Captor
	private ArgumentCaptor<Subscription> saved;

	private final User user = new User("ada@example.com", "hash");

	private SubscriptionService subscriptionService;

	@BeforeEach
	void setUp() {
		subscriptionService = new SubscriptionService(subscriptionRepository, transactionRepository, userRepository,
				OCT_25);
	}

	@Test
	void refreshAddsNewUpdatesKnownAndDeactivatesUndetected() {
		List<Transaction> history = new ArrayList<>();
		history.addAll(monthly("Touchstone Climbing", null, "78.50", "PERSONAL_CARE", LocalDate.of(2026, 8, 10)));
		history.addAll(monthly(null, "SPOTIFY USA", "11.99", "ENTERTAINMENT", LocalDate.of(2026, 8, 1)));
		// Recurring, but a credit card payment isn't a subscription
		history.addAll(monthly("Chase", null, "25.00", "LOAN_PAYMENTS", LocalDate.of(2026, 8, 12)));
		// Recurring but stopped: last charge in June, two months missed by Oct 25
		history.addAll(monthly("Old Gym", null, "30.00", "PERSONAL_CARE", LocalDate.of(2026, 4, 5)));
		Subscription knownClimbing = new Subscription(user, "touchstone climbing", "Touchstone Climbing",
				new BigDecimal("70.00"), "USD", Frequency.MONTHLY, LocalDate.of(2026, 7, 10), "PERSONAL_CARE", true);
		Subscription cancelledMagazine = new Subscription(user, "magazine", "Magazine", new BigDecimal("5.00"), "USD",
				Frequency.MONTHLY, LocalDate.of(2026, 9, 1), null, true);
		given(transactionRepository.findByUserIdAndAmountGreaterThan(42L, BigDecimal.ZERO)).willReturn(history);
		given(subscriptionRepository.findByUserId(42L)).willReturn(List.of(knownClimbing, cancelledMagazine));
		given(userRepository.getReferenceById(42L)).willReturn(user);
		given(subscriptionRepository.save(saved.capture())).willAnswer((invocation) -> invocation.getArgument(0));

		subscriptionService.refresh(42L);

		assertThat(knownClimbing.getAmount()).isEqualByComparingTo("78.50");
		assertThat(knownClimbing.getLastChargedOn()).isEqualTo(LocalDate.of(2026, 10, 10));
		assertThat(knownClimbing.isActive()).isTrue();
		assertThat(cancelledMagazine.isActive()).isFalse();
		assertThat(saved.getAllValues()).extracting(Subscription::getMerchantName, Subscription::isActive)
			.containsExactlyInAnyOrder(tuple("SPOTIFY USA", true),
					tuple("Old Gym", false));
	}

	@Test
	void refreshWithNoHistoryDeactivatesEverything() {
		Subscription known = new Subscription(user, "gym", "Gym", BigDecimal.TEN, "USD", Frequency.MONTHLY,
				LocalDate.of(2026, 10, 1), null, true);
		given(transactionRepository.findByUserIdAndAmountGreaterThan(42L, BigDecimal.ZERO)).willReturn(List.of());
		given(subscriptionRepository.findByUserId(42L)).willReturn(List.of(known));

		subscriptionService.refresh(42L);

		assertThat(known.isActive()).isFalse();
		then(subscriptionRepository).should(never()).save(any());
	}

	@Test
	void listShowsActiveFirstThenHighestMonthlyCost() {
		given(subscriptionRepository.findByUserId(42L)).willReturn(List.of(
				subscription("Domain", "120.00", Frequency.ANNUAL, true),
				subscription("Old Gym", "300.00", Frequency.MONTHLY, false),
				subscription("Meal Kit", "60.00", Frequency.WEEKLY, true),
				subscription("Climbing", "78.50", Frequency.MONTHLY, true)));

		List<SubscriptionResponse> ledger = subscriptionService.listForUser(42L);

		assertThat(ledger).extracting(SubscriptionResponse::merchantName)
			.containsExactly("Meal Kit", "Climbing", "Domain", "Old Gym");
		assertThat(ledger.get(0).monthlyCost()).isEqualByComparingTo("260.00");
		assertThat(ledger.get(2).monthlyCost()).isEqualByComparingTo("10.00");
	}

	private List<Transaction> monthly(String merchantName, String name, String amount, String category,
			LocalDate first) {
		List<Transaction> charges = new ArrayList<>();
		for (int month = 0; month < 3; month++) {
			charges.add(new Transaction(user, null, merchantName + name + month, new BigDecimal(amount), "USD",
					first.plusMonths(month), (name != null) ? name : merchantName.toUpperCase(), merchantName,
					category, false));
		}
		return charges;
	}

	private Subscription subscription(String merchant, String amount, Frequency frequency, boolean active) {
		return new Subscription(user, merchant.toLowerCase(), merchant, new BigDecimal(amount), "USD", frequency,
				LocalDate.of(2026, 10, 1), null, active);
	}

}
