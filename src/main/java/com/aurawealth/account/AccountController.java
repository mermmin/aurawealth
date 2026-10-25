package com.aurawealth.account;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccountController {

	private final AccountService accountService;

	public AccountController(AccountService accountService) {
		this.accountService = accountService;
	}

	@GetMapping("/api/accounts")
	public List<AccountResponse> list(@AuthenticationPrincipal Jwt jwt) {
		return accountService.listForUser(Long.valueOf(jwt.getSubject()));
	}

}
