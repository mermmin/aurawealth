package com.aurawealth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.util.ReflectionTestUtils;

import com.aurawealth.user.User;

class JwtServiceTests {

	private static final String SECRET = "test-secret-that-is-at-least-32-bytes-long";

	private final SecurityConfig securityConfig = new SecurityConfig();

	private final JwtService jwtService = new JwtService(securityConfig.jwtEncoder(SECRET), Duration.ofHours(1));

	private final JwtDecoder jwtDecoder = securityConfig.jwtDecoder(SECRET);

	@Test
	void issuedTokenDecodesWithUserClaimsAndOneHourExpiry() {
		Jwt issued = jwtService.issue(user(42L, "ada@example.com"));

		Jwt decoded = jwtDecoder.decode(issued.getTokenValue());

		assertThat(decoded.getSubject()).isEqualTo("42");
		assertThat(decoded.getClaimAsString("email")).isEqualTo("ada@example.com");
		assertThat(decoded.getClaimAsString("iss")).isEqualTo("aurawealth");
		assertThat(Duration.between(decoded.getIssuedAt(), decoded.getExpiresAt())).isEqualTo(Duration.ofHours(1));
	}

	@Test
	void tokenSignedWithAnotherSecretIsRejected() {
		JwtService otherService = new JwtService(
				securityConfig.jwtEncoder("a-completely-different-secret-of-32-bytes"), Duration.ofHours(1));
		String forged = otherService.issue(user(42L, "ada@example.com")).getTokenValue();

		assertThatThrownBy(() -> jwtDecoder.decode(forged)).isInstanceOf(JwtException.class);
	}

	@Test
	void expiredTokenIsRejected() {
		Instant twoHoursAgo = Instant.now().minus(Duration.ofHours(2));
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer("aurawealth")
			.subject("42")
			.issuedAt(twoHoursAgo)
			.expiresAt(twoHoursAgo.plus(Duration.ofHours(1)))
			.build();
		String expired = securityConfig.jwtEncoder(SECRET)
			.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
			.getTokenValue();

		assertThatThrownBy(() -> jwtDecoder.decode(expired)).isInstanceOf(JwtException.class);
	}

	@Test
	void shortSecretIsRefused() {
		assertThatThrownBy(() -> securityConfig.jwtEncoder("too-short")).isInstanceOf(IllegalStateException.class);
	}

	private static User user(Long id, String email) {
		User user = new User(email, "hash");
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}

}
