package com.aurawealth.security;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.aurawealth.user.User;

@Service
public class JwtService {

	static final String ISSUER = "aurawealth";

	private final JwtEncoder jwtEncoder;

	private final Duration ttl;

	public JwtService(JwtEncoder jwtEncoder, @Value("${aurawealth.jwt.ttl}") Duration ttl) {
		this.jwtEncoder = jwtEncoder;
		this.ttl = ttl;
	}

	/** Issues an access token whose subject is the user's id. */
	public Jwt issue(User user) {
		Instant now = Instant.now();
		JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
			.issuer(ISSUER)
			.subject(user.getId().toString())
			.issuedAt(now)
			.expiresAt(now.plus(ttl))
			.claim("email", user.getEmail());
		if (user.getDisplayName() != null) {
			claims.claim("name", user.getDisplayName());
		}
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims.build()));
	}

}
