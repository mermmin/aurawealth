package com.aurawealth.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

// Runs against the real Postgres from docker-compose (Flyway applies migrations); each test rolls back.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTests {

	@Autowired
	private UserRepository userRepository;

	@Test
	void savesUserAndFindsByEmail() {
		User saved = userRepository.saveAndFlush(new User("  Ada@Example.com ", "hash"));

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getEmail()).isEqualTo("ada@example.com");
		assertThat(saved.getCreatedAt()).isNotNull();
		assertThat(userRepository.findByEmail("ada@example.com")).get()
			.extracting(User::getId)
			.isEqualTo(saved.getId());
		assertThat(userRepository.existsByEmail("nobody@example.com")).isFalse();
	}

	@Test
	void rejectsDuplicateEmail() {
		userRepository.saveAndFlush(new User("ada@example.com", "hash"));

		assertThatThrownBy(() -> userRepository.saveAndFlush(new User("ADA@example.com", "other")))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

}
