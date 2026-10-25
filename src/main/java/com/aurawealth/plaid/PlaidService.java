package com.aurawealth.plaid;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import com.aurawealth.account.Account;
import com.aurawealth.account.AccountRepository;
import com.aurawealth.account.AccountResponse;
import com.aurawealth.subscription.SubscriptionService;
import com.aurawealth.transaction.Transaction;
import com.aurawealth.transaction.TransactionRepository;
import com.aurawealth.user.User;
import com.aurawealth.user.UserRepository;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
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
import com.plaid.client.model.PlaidError;
import com.plaid.client.model.Products;
import com.plaid.client.model.RemovedTransaction;
import com.plaid.client.model.TransactionsSyncRequest;
import com.plaid.client.model.TransactionsSyncResponse;
import com.plaid.client.request.PlaidApi;

import retrofit2.Call;
import retrofit2.Response;

@Service
public class PlaidService {

	static final String MUTATION_DURING_PAGINATION = "TRANSACTIONS_SYNC_MUTATION_DURING_PAGINATION";

	static final int MAX_SYNC_ATTEMPTS = 3;

	private static final int SYNC_PAGE_SIZE = 500;

	private static final Gson GSON = new Gson();

	private final PlaidApi plaidApi;

	private final UserRepository userRepository;

	private final PlaidItemRepository plaidItemRepository;

	private final AccountRepository accountRepository;

	private final TransactionRepository transactionRepository;

	private final SubscriptionService subscriptionService;

	private final TransactionOperations transactionOperations;

	private final Clock clock;

	public PlaidService(PlaidApi plaidApi, UserRepository userRepository, PlaidItemRepository plaidItemRepository,
			AccountRepository accountRepository, TransactionRepository transactionRepository,
			SubscriptionService subscriptionService, TransactionOperations transactionOperations, Clock clock) {
		this.plaidApi = plaidApi;
		this.userRepository = userRepository;
		this.plaidItemRepository = plaidItemRepository;
		this.accountRepository = accountRepository;
		this.transactionRepository = transactionRepository;
		this.subscriptionService = subscriptionService;
		this.transactionOperations = transactionOperations;
		this.clock = clock;
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
			PlaidItem newItem = new PlaidItem(user, exchange.getItemId(), exchange.getAccessToken(), institutionName);
			newItem.markSynced(Instant.now(clock));
			PlaidItem item = plaidItemRepository.save(newItem);
			return balances.getAccounts()
				.stream()
				.map((plaidAccount) -> AccountResponse.from(accountRepository.save(upsert(user, item, plaidAccount))))
				.toList();
		});
	}

	/**
	 * Refreshes balances and pulls new, changed and removed transactions for every bank the user has linked, then
	 * re-detects subscriptions from the updated history.
	 */
	public SyncResponse sync(Long userId) {
		SyncResponse total = SyncResponse.NONE;
		for (PlaidItem item : plaidItemRepository.findByUserId(userId)) {
			total = total.plus(syncItem(userId, item));
		}
		subscriptionService.refresh(userId);
		return total;
	}

	/** Same shape as {@link #linkItem}: all Plaid calls first, then one transaction applies everything. */
	private SyncResponse syncItem(Long userId, PlaidItem item) {
		AccountsGetResponse balances = execute(
				plaidApi.accountsBalanceGet(new AccountsBalanceGetRequest().accessToken(item.getAccessToken())));
		TransactionChanges changes = fetchTransactionChanges(item.getAccessToken(), item.getTransactionsCursor());

		return transactionOperations.execute((status) -> {
			User user = userRepository.getReferenceById(userId);
			PlaidItem managedItem = plaidItemRepository.findById(item.getId()).orElseThrow();
			Map<String, Account> accountsByPlaidId = balances.getAccounts()
				.stream()
				.map((plaidAccount) -> accountRepository.save(upsert(user, managedItem, plaidAccount)))
				.collect(Collectors.toMap(Account::getPlaidAccountId, Function.identity()));
			changes.added().forEach((plaidTransaction) -> upsert(user, accountsByPlaidId, plaidTransaction));
			changes.modified().forEach((plaidTransaction) -> upsert(user, accountsByPlaidId, plaidTransaction));
			int removed = changes.removedIds().isEmpty() ? 0
					: transactionRepository.deleteByPlaidTransactionIds(changes.removedIds());
			// Saved last and in the same transaction, so a failed save means the next sync re-fetches these changes
			managedItem.updateTransactionsCursor(changes.nextCursor());
			managedItem.markSynced(Instant.now(clock));
			return new SyncResponse(accountsByPlaidId.size(), changes.added().size(), changes.modified().size(),
					removed);
		});
	}

	/**
	 * Pages through /transactions/sync until {@code has_more} is false. If the bank's data changes mid-way, Plaid
	 * rejects the next page and the documented fix is to restart the whole run from the cursor we started with.
	 */
	private TransactionChanges fetchTransactionChanges(String accessToken, String startCursor) {
		for (int attempt = 1;; attempt++) {
			try {
				return fetchAllPages(accessToken, startCursor);
			}
			catch (PlaidException ex) {
				if (!MUTATION_DURING_PAGINATION.equals(ex.getErrorCode()) || attempt == MAX_SYNC_ATTEMPTS) {
					throw ex;
				}
			}
		}
	}

	private TransactionChanges fetchAllPages(String accessToken, String cursor) {
		List<com.plaid.client.model.Transaction> added = new ArrayList<>();
		List<com.plaid.client.model.Transaction> modified = new ArrayList<>();
		List<String> removedIds = new ArrayList<>();
		boolean hasMore = true;
		while (hasMore) {
			TransactionsSyncResponse page = execute(plaidApi.transactionsSync(
					new TransactionsSyncRequest().accessToken(accessToken).cursor(cursor).count(SYNC_PAGE_SIZE)));
			added.addAll(page.getAdded());
			modified.addAll(page.getModified());
			page.getRemoved().stream().map(RemovedTransaction::getTransactionId).forEach(removedIds::add);
			cursor = page.getNextCursor();
			hasMore = Boolean.TRUE.equals(page.getHasMore());
		}
		return new TransactionChanges(added, modified, removedIds, cursor);
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

	// Added and modified are both upserts, so re-running a sync (e.g. after a lost cursor) never duplicates rows
	private void upsert(User user, Map<String, Account> accountsByPlaidId,
			com.plaid.client.model.Transaction plaidTransaction) {
		Account account = accountsByPlaidId.get(plaidTransaction.getAccountId());
		if (account == null) {
			// Plaid only sends transactions for the item's accounts, which were all just upserted above
			throw new PlaidException("Transaction for unknown account " + plaidTransaction.getAccountId());
		}
		String category = (plaidTransaction.getPersonalFinanceCategory() != null)
				? plaidTransaction.getPersonalFinanceCategory().getPrimary() : null;
		String name = Objects.requireNonNullElse(plaidTransaction.getName(), "Unknown");
		BigDecimal amount = toDecimal(plaidTransaction.getAmount());
		boolean pending = Boolean.TRUE.equals(plaidTransaction.getPending());
		Transaction transaction = transactionRepository.findByPlaidTransactionId(plaidTransaction.getTransactionId())
			.map((existing) -> {
				existing.update(amount, plaidTransaction.getIsoCurrencyCode(), plaidTransaction.getDate(), name,
						plaidTransaction.getMerchantName(), category, pending);
				return existing;
			})
			.orElseGet(() -> new Transaction(user, account, plaidTransaction.getTransactionId(), amount,
					plaidTransaction.getIsoCurrencyCode(), plaidTransaction.getDate(), name,
					plaidTransaction.getMerchantName(), category, pending));
		transactionRepository.save(transaction);
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
				throw new PlaidException("Plaid request failed with HTTP " + response.code() + ": " + error,
						errorCode(error));
			}
			return response.body();
		}
		catch (IOException ex) {
			throw new PlaidException("Could not reach Plaid", ex);
		}
	}

	private static String errorCode(String errorBody) {
		try {
			PlaidError error = GSON.fromJson(errorBody, PlaidError.class);
			return (error != null) ? error.getErrorCode() : null;
		}
		catch (JsonParseException ex) {
			return null;
		}
	}

	private record TransactionChanges(List<com.plaid.client.model.Transaction> added,
			List<com.plaid.client.model.Transaction> modified, List<String> removedIds, String nextCursor) {
	}

}
