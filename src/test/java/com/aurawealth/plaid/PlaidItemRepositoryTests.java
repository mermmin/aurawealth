package com.aurawealth.plaid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import com.aurawealth.security.EncryptedStringConverter;
import com.aurawealth.user.User;
import com.aurawealth.user.UserRepository;

// Runs against the real Postgres from docker-compose (Flyway applies migrations); each test rolls back.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(EncryptedStringConverter.class)
class PlaidItemRepositoryTests {

	private static final String TOKEN = "access-sandbox-de3ce8ef-33f8-452c-a685-8671031fc0f6";

	@Autowired
	private PlaidItemRepository plaidItemRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void savesItemWithAccessTokenEncryptedInTheDatabase() {
		User user = userRepository.save(new User("ada@example.com", "hash"));
		PlaidItem saved = plaidItemRepository.saveAndFlush(new PlaidItem(user, "item-1", TOKEN, "First Platypus Bank"));
		entityManager.clear();

		Object rawColumn = entityManager.getEntityManager()
			.createNativeQuery("SELECT access_token FROM plaid_items WHERE id = :id")
			.setParameter("id", saved.getId())
			.getSingleResult();
		assertThat(rawColumn).asString().isNotBlank().doesNotContain(TOKEN);

		PlaidItem found = plaidItemRepository.findByItemId("item-1").orElseThrow();
		assertThat(found.getAccessToken()).isEqualTo(TOKEN);
		assertThat(found.getInstitutionName()).isEqualTo("First Platypus Bank");
		assertThat(found.getCreatedAt()).isNotNull();
		assertThat(plaidItemRepository.findByUserId(user.getId())).extracting(PlaidItem::getId)
			.containsExactly(saved.getId());
	}

	@Test
	void rejectsDuplicateItemId() {
		User user = userRepository.save(new User("ada@example.com", "hash"));
		plaidItemRepository.saveAndFlush(new PlaidItem(user, "item-1", TOKEN, null));

		assertThatThrownBy(() -> plaidItemRepository.saveAndFlush(new PlaidItem(user, "item-1", "other", null)))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

}
