package com.aurawealth.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTests {

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@InjectMocks
	private UserService userService;

	@Test
	void registerStoresNormalizedEmailAndHashedPassword() {
		given(userRepository.existsByEmail("ada@example.com")).willReturn(false);
		given(passwordEncoder.encode("s3cret-pass")).willReturn("hashed");
		given(userRepository.saveAndFlush(any(User.class))).willAnswer((invocation) -> invocation.getArgument(0));

		User user = userService.register(" Ada@Example.com ", "s3cret-pass");

		assertThat(user.getEmail()).isEqualTo("ada@example.com");
		assertThat(user.getPasswordHash()).isEqualTo("hashed");
	}

	@Test
	void registerRejectsExistingEmail() {
		given(userRepository.existsByEmail("ada@example.com")).willReturn(true);

		assertThatThrownBy(() -> userService.register("ADA@example.com", "s3cret-pass"))
			.isInstanceOf(EmailAlreadyRegisteredException.class);
		verify(userRepository, never()).saveAndFlush(any());
		verify(passwordEncoder, never()).encode(any());
	}

	@Test
	void registerTranslatesUniqueConstraintRace() {
		given(userRepository.existsByEmail("ada@example.com")).willReturn(false);
		given(passwordEncoder.encode("s3cret-pass")).willReturn("hashed");
		given(userRepository.saveAndFlush(any(User.class))).willThrow(new DataIntegrityViolationException("dup"));

		assertThatThrownBy(() -> userService.register("ada@example.com", "s3cret-pass"))
			.isInstanceOf(EmailAlreadyRegisteredException.class);
	}

}
