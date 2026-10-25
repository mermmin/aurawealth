package com.aurawealth.account;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountResponse(Long id, String institutionName, String name, String mask, String type, String subtype,
		BigDecimal currentBalance, BigDecimal availableBalance, String isoCurrencyCode, Instant updatedAt) {

	/** Reads the account's Plaid item, so call it inside a transaction or after fetching the item eagerly. */
	public static AccountResponse from(Account account) {
		return new AccountResponse(account.getId(), account.getPlaidItem().getInstitutionName(), account.getName(),
				account.getMask(), account.getType(), account.getSubtype(), account.getCurrentBalance(),
				account.getAvailableBalance(), account.getIsoCurrencyCode(), account.getUpdatedAt());
	}

}
