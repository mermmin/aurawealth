package com.aurawealth.plaid;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import com.aurawealth.account.Account;
import com.aurawealth.account.AccountRepository;
import com.aurawealth.account.AccountResponse;
import com.aurawealth.user.User;
import com.aurawealth.user.UserRepository;
import com.plaid.client.model.AccountBase;
import com.plaid.client.model.AccountBalance;
import com.plaid.client.model.AccountsBalanceGetRequest;
import com.plaid.client.model.AccountsGetResponse;
import com.plaid.client.model.CountryCode;
import com.plaid.client.model.ItemPublicTokenExchangeRequest;
import com.plaid.client.model.ItemPublicTokenExchangeResponse;
import com.plaid.client.model.LinkTokenCreateRequest;
import com.plaid.client.model.LinkTokenCreateRequestUser;
import com.plaid.client.model.LinkTokenCreateResponse;
import com.plaid.client.model.Products;
import com.plaid.client.request.PlaidApi;

import retrofit2.Call;
import retrofit2.Response;

@Service
public class PlaidService {

	private final PlaidApi plaidApi;

	private final UserRepository userRepository;

	private final PlaidItemRepository plaidItemRepository;

	private final AccountRepository accountRepository;

	private final TransactionOperations transactionOperations;

	public PlaidService(PlaidApi plaidApi, UserRepository userRepository, PlaidItemRepository plaidItemRepository,
			AccountRepository accountRepository, TransactionOperations transactionOperations) {
		this.plaidApi = plaidApi;
		this.userRepository = userRepository;
		this.plaidItemRepository = plaidItemRepository;
		this.accountRepository = accountRepository;
		this.transactionOperations = transactionOperations;
	}

	/** Creates a short-lived token the frontend uses to open Plaid Link for this user. */
	public LinkTokenResponse createLinkToken(Long userId) {
		LinkTokenCreateRequest request = new LinkTokenCreateRequest()
			.user(new LinkTokenCreateRequestUser().clientUserId(userId.toString()))
			.clientName("AuraWealth")
			.products(List.of(Products.TRANSACTIONS))
			.countryCodes(List.of(CountryCode.US))
			.language("en");
		LinkTokenCreateResponse response = execute(plaidApi.linkTokenCreate(request));
		return new LinkTokenResponse(response.getLinkToken(), response.getExpiration());
	}

	/**
	 * Finishes a Plaid Link flow: trades the one-time public token for a long-lived access token, fetches the
	 * item's accounts with live balances, and saves both. Plaid calls happen before the transaction opens, so a slow
	 * bank never holds a database connection, and a failed save leaves no half-linked item behind.
	 */
	public List<AccountResponse> linkItem(Long userId, String publicToken) {
		ItemPublicTokenExchangeResponse exchange = execute(
				plaidApi.itemPublicTokenExchange(new ItemPublicTokenExchangeRequest().publicToken(publicToken)));
		AccountsGetResponse balances = execute(
				plaidApi.accountsBalanceGet(new AccountsBalanceGetRequest().accessToken(exchange.getAccessToken())));

		return transactionOperations.execute((status) -> {
			User user = userRepository.getReferenceById(userId);
			String institutionName = (balances.getItem() != null) ? balances.getItem().getInstitutionName() : null;
			PlaidItem item = plaidItemRepository
				.save(new PlaidItem(user, exchange.getItemId(), exchange.getAccessToken(), institutionName));
			return balances.getAccounts()
				.stream()
				.map((plaidAccount) -> AccountResponse.from(accountRepository.save(upsert(user, item, plaidAccount))))
				.toList();
		});
	}

	private Account upsert(User user, PlaidItem item, AccountBase plaidAccount) {
		Account account = accountRepository.findByPlaidAccountId(plaidAccount.getAccountId())
			.orElseGet(() -> new Account(user, item, plaidAccount.getAccountId(), plaidAccount.getName(),
					plaidAccount.getMask(), plaidAccount.getType().getValue(),
					(plaidAccount.getSubtype() != null) ? plaidAccount.getSubtype().getValue() : null));
		AccountBalance balance = plaidAccount.getBalances();
		account.updateBalances(toDecimal(balance.getCurrent()), toDecimal(balance.getAvailable()),
				balance.getIsoCurrencyCode());
		return account;
	}

	// BigDecimal.valueOf uses the double's shortest decimal form, so 100.1 stays 100.1 rather than 100.0999...
	private static BigDecimal toDecimal(Double amount) {
		return (amount != null) ? BigDecimal.valueOf(amount) : null;
	}

	private static <T> T execute(Call<T> call) {
		try {
			Response<T> response = call.execute();
			if (!response.isSuccessful()) {
				String error = (response.errorBody() != null) ? response.errorBody().string() : "";
				throw new PlaidException("Plaid request failed with HTTP " + response.code() + ": " + error);
			}
			return response.body();
		}
		catch (IOException ex) {
			throw new PlaidException("Could not reach Plaid", ex);
		}
	}

}
