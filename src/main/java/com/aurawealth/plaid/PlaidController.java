package com.aurawealth.plaid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/plaid")
public class PlaidController {

	private final PlaidService plaidService;

	public PlaidController(PlaidService plaidService) {
		this.plaidService = plaidService;
	}

	@PostMapping("/link-token")
	public LinkTokenResponse createLinkToken(@AuthenticationPrincipal Jwt jwt) {
		return plaidService.createLinkToken(Long.valueOf(jwt.getSubject()));
	}

}
