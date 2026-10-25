package com.aurawealth.auth;

import java.time.Instant;

import org.springframework.security.oauth2.jwt.Jwt;

public record AuthResponse(String accessToken, String tokenType, Instant expiresAt) {

	static AuthResponse from(Jwt jwt) {
		return new AuthResponse(jwt.getTokenValue(), "Bearer", jwt.getExpiresAt());
	}

}
