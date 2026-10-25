package com.aurawealth.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import com.aurawealth.account.Account;
import com.aurawealth.account.AccountRepository;
import com.aurawealth.plaid.PlaidItem;
import com.aurawealth.plaid.PlaidItemRepository;
import com.aurawealth.security.EncryptedStringConverter;
import com.aurawealth.user.User;
import com.aurawealth.user.UserRepository;

// Runs against the real Postgres from docker-compose (Flyway applies migrations); each test rolls back.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(EncryptedStringConverter.class)
class TransactionRepositoryTests {

	private static final LocalDate OCT_1 = LocalDate.of(2026, 10, 1);

	@Autowired
	private TransactionRepository transactionRepository;

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private PlaidItemRepository plaidItemRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private TestEntityManager entityManager;

	private User user;

	private Account account;

	@BeforeEach
	void setUp() {
		user = userRepository.save(new User("ada@example.com", "hash"));
		PlaidItem item = plaidItemRepository.save(new PlaidItem(user, "item-1", "access-sandbox-token", null));
		account = accountRepository
			.save(new Account(user, item, "acc-checking", "Plaid Checking", "0000", "depository", "checking"));
	}

	@Test
	void savesTransactionWithExactAmountAndDate() {
		transactionRepository.saveAndFlush(transaction("tx-netflix", "15.4900", "Netflix", "ENTERTAINMENT"));
		transactionRepository.saveAndFlush(transaction("tx-refund", "-25.0000", "Refund", null));
		entityManager.clear();

		Transaction netflix = transactionRepository.findByPlaidTransactionId("tx-netflix").orElseThrow();
		assertThat(netflix.getAmount()).isEqualByComparingTo("15.49");
		assertThat(netflix.getDate()).isEqualTo(OCT_1);
		assertThat(netflix.getMerchantName()).isEqualTo("Netflix");
		assertThat(netflix.getCategory()).isEqualTo("ENTERTAINMENT");
		assertThat(netflix.isPending()).isTrue();
		assertThat(netflix.getAccount().getId()).isEqualTo(account.getId());
		assertThat(netflix.getCreatedAt()).isNotNull();
		assertThat(transactionRepository.findByPlaidTransactionId("tx-refund").orElseThrow().getAmount())
			.isEqualByComparingTo("-25");
	}

	@Test
	void appliesModifiedVersionFromPlaid() {
		Transaction saved = transactionRepository.saveAndFlush(transaction("tx-1", "15.4900", "Netflix", null));

		saved.update(new BigDecimal("17.9900"), "USD", OCT_1.plusDays(2), "NETFLIX.COM", "Netflix", "ENTERTAINMENT",
				false);
		transactionRepository.saveAndFlush(saved);
		entityManager.clear();

		Transaction found = transactionRepository.findByPlaidTransactionId("tx-1").orElseThrow();
		assertThat(found.getAmount()).isEqualByComparingTo("17.99");
		assertThat(found.getDate()).isEqualTo(OCT_1.plusDays(2));
		assertThat(found.isPending()).isFalse();
	}

	@Test
	void deletesRemovedTransactionsByPlaidId() {
		transactionRepository.saveAndFlush(transaction("tx-1", "1", "A", null));
		transactionRepository.saveAndFlush(transaction("tx-2", "2", "B", null));
		transactionRepository.saveAndFlush(transaction("tx-3", "3", "C", null));

		int deleted = transactionRepository.deleteByPlaidTransactionIds(List.of("tx-1", "tx-3", "tx-unknown"));
		entityManager.clear();

		// Checks only this test's rows: the dev database may also hold transactions from live sandbox syncs
		assertThat(deleted).isEqualTo(2);
		assertThat(transactionRepository.findByPlaidTransactionId("tx-1")).isEmpty();
		assertThat(transactionRepository.findByPlaidTransactionId("tx-2")).isPresent();
		assertThat(transactionRepository.findByPlaidTransactionId("tx-3")).isEmpty();
	}

	@Test
	void rejectsDuplicatePlaidTransactionId() {
		transactionRepository.saveAndFlush(transaction("tx-1", "1", "A", null));

		assertThatThrownBy(() -> transactionRepository.saveAndFlush(transaction("tx-1", "2", "B", null)))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void deletingAnAccountDeletesItsTransactions() {
		transactionRepository.saveAndFlush(transaction("tx-1", "1", "A", null));
		entityManager.clear();

		// Plain SQL so the database's ON DELETE CASCADE does the work, not Hibernate
		entityManager.getEntityManager()
			.createNativeQuery("DELETE FROM accounts WHERE id = :id")
			.setParameter("id", account.getId())
			.executeUpdate();

		assertThat(transactionRepository.findByPlaidTransactionId("tx-1")).isEmpty();
	}

	private Transaction transaction(String plaidId, String amount, String merchantName, String category) {
		return new Transaction(user, account, plaidId, new BigDecimal(amount), "USD", OCT_1, merchantName.toUpperCase(),
				merchantName, category, true);
	}

}
