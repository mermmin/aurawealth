package com.aurawealth.user;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

	/** {@code displayName} is null when the user didn't give one; the UI then falls back to the email. */
	public record MeResponse(Long id, String email, String displayName) {
	}

	/** The signed-in user, read straight from the verified token (no database hit). */
	@GetMapping("/api/me")
	public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
		return new MeResponse(Long.valueOf(jwt.getSubject()), jwt.getClaimAsString("email"),
				jwt.getClaimAsString("name"));
	}

}
