package com.aurawealth.plaid;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.aurawealth.account.AccountResponse;

import jakarta.validation.Valid;

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

	/** Called after Plaid Link succeeds in the browser; returns the newly linked accounts. */
	@PostMapping("/exchange")
	@ResponseStatus(HttpStatus.CREATED)
	public List<AccountResponse> exchange(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody PublicTokenExchangeRequest request) {
		return plaidService.linkItem(Long.valueOf(jwt.getSubject()), request.publicToken());
	}

	/** Refreshes balances and transactions for all of the signed-in user's linked banks. */
	@PostMapping("/sync")
	public SyncResponse sync(@AuthenticationPrincipal Jwt jwt) {
		return plaidService.sync(Long.valueOf(jwt.getSubject()));
	}

}
