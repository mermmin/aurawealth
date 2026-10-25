package com.aurawealth.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import com.aurawealth.user.User;
import com.aurawealth.user.UserRepository;

// Runs against the real Postgres from docker-compose (Flyway applies migrations); each test rolls back.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SubscriptionRepositoryTests {

	private static final LocalDate OCT_9 = LocalDate.of(2026, 10, 9);

	@Autowired
	private SubscriptionRepository subscriptionRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private TestEntityManager entityManager;

	private User user;

	@BeforeEach
	void setUp() {
		user = userRepository.save(new User("ada@example.com", "hash"));
	}

	@Test
	void savesSubscriptionWithFrequencyAndNextExpectedDate() {
		subscriptionRepository.saveAndFlush(climbing(user, Frequency.MONTHLY));
		subscriptionRepository.saveAndFlush(new Subscription(user, "spotify", "Spotify", new BigDecimal("119.88"),
				"USD", Frequency.ANNUAL, LocalDate.of(2026, 2, 28), null, true));
		entityManager.clear();

		Subscription found = subscriptionRepository.findByUserId(user.getId())
			.stream()
			.filter((subscription) -> subscription.getMerchantKey().equals("touchstone climbing"))
			.findFirst()
			.orElseThrow();
		assertThat(found.getMerchantName()).isEqualTo("Touchstone Climbing");
		assertThat(found.getAmount()).isEqualByComparingTo("78.50");
		assertThat(found.getFrequency()).isEqualTo(Frequency.MONTHLY);
		assertThat(found.getLastChargedOn()).isEqualTo(OCT_9);
		assertThat(found.getNextExpectedOn()).isEqualTo(LocalDate.of(2026, 11, 9));
		assertThat(found.isActive()).isTrue();
		assertThat(found.getCreatedAt()).isNotNull();
		assertThat(subscriptionRepository.findByUserId(user.getId())).hasSize(2);
	}

	@Test
	void updateRecomputesNextExpectedAndDeactivatePersists() {
		Subscription saved = subscriptionRepository.saveAndFlush(climbing(user, Frequency.MONTHLY));

		saved.update("Touchstone Climbing", new BigDecimal("80.00"), "USD", Frequency.WEEKLY, OCT_9.plusDays(7),
				"PERSONAL_CARE", true);
		saved.deactivate();
		subscriptionRepository.saveAndFlush(saved);
		entityManager.clear();

		Subscription found = subscriptionRepository.findById(saved.getId()).orElseThrow();
		assertThat(found.getFrequency()).isEqualTo(Frequency.WEEKLY);
		assertThat(found.getNextExpectedOn()).isEqualTo(OCT_9.plusDays(14));
		assertThat(found.isActive()).isFalse();
	}

	@Test
	void merchantIsUniquePerUserButNotAcrossUsers() {
		subscriptionRepository.saveAndFlush(climbing(user, Frequency.MONTHLY));
		User other = userRepository.save(new User("bob@example.com", "hash"));
		subscriptionRepository.saveAndFlush(climbing(other, Frequency.MONTHLY));

		assertThatThrownBy(() -> subscriptionRepository.saveAndFlush(climbing(user, Frequency.WEEKLY)))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	private static Subscription climbing(User owner, Frequency frequency) {
		return new Subscription(owner, "touchstone climbing", "Touchstone Climbing", new BigDecimal("78.50"), "USD",
				frequency, OCT_9, "PERSONAL_CARE", true);
	}

}
