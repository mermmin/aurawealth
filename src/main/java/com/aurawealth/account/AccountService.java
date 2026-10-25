package com.aurawealth.account;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class AccountService {

	private final AccountRepository accountRepository;

	public AccountService(AccountRepository accountRepository) {
		this.accountRepository = accountRepository;
	}

	public List<AccountResponse> listForUser(Long userId) {
		return accountRepository.findByUserIdOrderByNameAsc(userId).stream().map(AccountResponse::from).toList();
	}

}
