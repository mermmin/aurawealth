package com.aurawealth.user;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

	private final UserRepository userRepository;

	private final PasswordEncoder passwordEncoder;

	public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public User register(String email, String rawPassword) {
		String normalizedEmail = User.normalizeEmail(email);
		if (userRepository.existsByEmail(normalizedEmail)) {
			throw new EmailAlreadyRegisteredException(normalizedEmail);
		}
		try {
			return userRepository.saveAndFlush(new User(normalizedEmail, passwordEncoder.encode(rawPassword)));
		}
		catch (DataIntegrityViolationException ex) {
			// Two signups with the same email raced past the exists check; the unique constraint caught it.
			throw new EmailAlreadyRegisteredException(normalizedEmail);
		}
	}

	/** Returns the user if the password matches; the same error for unknown email and wrong password. */
	@Transactional(readOnly = true)
	public User authenticate(String email, String rawPassword) {
		return userRepository.findByEmail(User.normalizeEmail(email))
			.filter((user) -> passwordEncoder.matches(rawPassword, user.getPasswordHash()))
			.orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
	}

}
