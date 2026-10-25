package com.aurawealth.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import com.aurawealth.plaid.PlaidItem;
import com.aurawealth.plaid.PlaidItemRepository;
import com.aurawealth.security.EncryptedStringConverter;
import com.aurawealth.user.User;
import com.aurawealth.user.UserRepository;

// Runs against the real Postgres from docker-compose (Flyway applies migrations); each test rolls back.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(EncryptedStringConverter.class)
class AccountRepositoryTests {

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private PlaidItemRepository plaidItemRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private TestEntityManager entityManager;

	private User user;

	private PlaidItem item;

	@BeforeEach
	void setUp() {
		user = userRepository.save(new User("ada@example.com", "hash"));
		item = plaidItemRepository.save(new PlaidItem(user, "item-1", "access-sandbox-token", "First Platypus Bank"));
	}

	@Test
	void savesAccountWithExactBalancesAndFindsByUser() {
		Account checking = new Account(user, item, "acc-checking", "Plaid Checking", "0000", "depository", "checking");
		checking.updateBalances(new BigDecimal("110.0000"), new BigDecimal("100.1234"), "USD");
		accountRepository.saveAndFlush(checking);
		accountRepository.saveAndFlush(new Account(user, item, "acc-credit", "Plaid Credit Card", "3333", "credit",
				"credit card"));
		entityManager.clear();

		Account found = accountRepository.findByPlaidAccountId("acc-checking").orElseThrow();
		assertThat(found.getCurrentBalance()).isEqualByComparingTo("110");
		assertThat(found.getAvailableBalance()).isEqualByComparingTo("100.1234");
		assertThat(found.getIsoCurrencyCode()).isEqualTo("USD");
		assertThat(found.getPlaidItem().getId()).isEqualTo(item.getId());
		assertThat(found.getUpdatedAt()).isNotNull();
		assertThat(accountRepository.findByUserIdOrderByNameAsc(user.getId())).extracting(Account::getName)
			.containsExactly("Plaid Checking", "Plaid Credit Card");
	}

	@Test
	void sumsCurrentBalancesByTypeForOneUserOnly() {
		saveAccount(user, item, "acc-checking", "depository", "110.0000");
		saveAccount(user, item, "acc-savings", "depository", "210.5000");
		saveAccount(user, item, "acc-card", "credit", "410.0000");
		saveAccount(user, item, "acc-cd", "investment", null);
		User other = userRepository.save(new User("bob@example.com", "hash"));
		PlaidItem otherItem = plaidItemRepository.save(new PlaidItem(other, "item-2", "token", null));
		saveAccount(other, otherItem, "acc-bob", "depository", "999.0000");

		assertThat(accountRepository.sumCurrentBalancesByType(user.getId()))
			.extracting(BalanceByType::type,
					(row) -> (row.total() != null) ? row.total().stripTrailingZeros().toPlainString() : null)
			.containsExactlyInAnyOrder(tuple("depository", "320.5"), tuple("credit", "410"),
					tuple("investment", null));
	}

	@Test
	void rejectsDuplicatePlaidAccountId() {
		accountRepository.saveAndFlush(new Account(user, item, "acc-1", "Checking", null, "depository", null));

		assertThatThrownBy(() -> accountRepository
			.saveAndFlush(new Account(user, item, "acc-1", "Again", null, "depository", null)))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	private void saveAccount(User owner, PlaidItem plaidItem, String plaidId, String type, String balance) {
		Account account = new Account(owner, plaidItem, plaidId, plaidId, null, type, null);
		account.updateBalances((balance != null) ? new BigDecimal(balance) : null, null, "USD");
		accountRepository.saveAndFlush(account);
	}

}
