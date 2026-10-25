package com.aurawealth.plaid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionOperations;

import com.aurawealth.account.Account;
import com.aurawealth.account.AccountRepository;
import com.aurawealth.account.AccountResponse;
import com.aurawealth.user.User;
import com.aurawealth.user.UserRepository;
import com.plaid.client.model.AccountBalance;
import com.plaid.client.model.AccountBase;
import com.plaid.client.model.AccountSubtype;
import com.plaid.client.model.AccountType;
import com.plaid.client.model.AccountsGetResponse;
import com.plaid.client.model.CountryCode;
import com.plaid.client.model.Item;
import com.plaid.client.model.ItemPublicTokenExchangeResponse;
import com.plaid.client.model.LinkTokenCreateRequest;
import com.plaid.client.model.LinkTokenCreateResponse;
import com.plaid.client.model.Products;
import com.plaid.client.request.PlaidApi;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

@ExtendWith(MockitoExtension.class)
class PlaidServiceTests {

	@Mock
	private PlaidApi plaidApi;

	@Mock
	private UserRepository userRepository;

	@Mock
	private PlaidItemRepository plaidItemRepository;

	@Mock
	private AccountRepository accountRepository;

	@Mock
	private Call<LinkTokenCreateResponse> call;

	@Mock
	private Call<ItemPublicTokenExchangeResponse> exchangeCall;

	@Mock
	private Call<AccountsGetResponse> balanceCall;

	@Captor
	private ArgumentCaptor<LinkTokenCreateRequest> request;

	@Captor
	private ArgumentCaptor<PlaidItem> savedItem;

	private PlaidService plaidService;

	@BeforeEach
	void setUp() {
		plaidService = new PlaidService(plaidApi, userRepository, plaidItemRepository, accountRepository,
				TransactionOperations.withoutTransaction());
	}

	@Test
	void createLinkTokenSendsUserIdAndReturnsToken() throws IOException {
		OffsetDateTime expiration = OffsetDateTime.parse("2026-10-25T06:00:00Z");
		given(plaidApi.linkTokenCreate(request.capture())).willReturn(call);
		given(call.execute()).willReturn(Response.success(
				new LinkTokenCreateResponse().linkToken("link-sandbox-123").expiration(expiration)));

		LinkTokenResponse response = plaidService.createLinkToken(42L);

		assertThat(response.linkToken()).isEqualTo("link-sandbox-123");
		assertThat(response.expiration()).isEqualTo(expiration);
		assertThat(request.getValue().getUser().getClientUserId()).isEqualTo("42");
		assertThat(request.getValue().getProducts()).isEqualTo(List.of(Products.TRANSACTIONS));
		assertThat(request.getValue().getCountryCodes()).isEqualTo(List.of(CountryCode.US));
	}

	@Test
	void createLinkTokenWrapsPlaidErrorResponse() throws IOException {
		given(plaidApi.linkTokenCreate(any())).willReturn(call);
		given(call.execute()).willReturn(Response.error(400, ResponseBody.create(
				"{\"error_code\":\"INVALID_API_KEYS\"}", MediaType.get("application/json"))));

		assertThatThrownBy(() -> plaidService.createLinkToken(42L))
			.isInstanceOf(PlaidException.class)
			.hasMessageContaining("INVALID_API_KEYS");
	}

	@Test
	void createLinkTokenWrapsNetworkFailure() throws IOException {
		given(plaidApi.linkTokenCreate(any())).willReturn(call);
		given(call.execute()).willThrow(new IOException("timeout"));

		assertThatThrownBy(() -> plaidService.createLinkToken(42L))
			.isInstanceOf(PlaidException.class)
			.hasCauseInstanceOf(IOException.class);
	}

	@Test
	void linkItemSavesItemWithAccessTokenAndNewAccounts() throws IOException {
		User user = new User("ada@example.com", "hash");
		givenPlaidReturns(
				plaidAccount("acc-checking", AccountType.DEPOSITORY, AccountSubtype.CHECKING, 110.0, 100.1),
				plaidAccount("acc-credit", AccountType.CREDIT, null, 410.0, null));
		given(userRepository.getReferenceById(42L)).willReturn(user);
		given(plaidItemRepository.save(savedItem.capture())).willAnswer((invocation) -> invocation.getArgument(0));
		given(accountRepository.findByPlaidAccountId(any())).willReturn(Optional.empty());
		given(accountRepository.save(any())).willAnswer((invocation) -> invocation.getArgument(0));

		List<AccountResponse> accounts = plaidService.linkItem(42L, "public-sandbox-abc");

		assertThat(savedItem.getValue().getUser()).isSameAs(user);
		assertThat(savedItem.getValue().getItemId()).isEqualTo("item-1");
		assertThat(savedItem.getValue().getAccessToken()).isEqualTo("access-sandbox-xyz");
		assertThat(savedItem.getValue().getInstitutionName()).isEqualTo("First Platypus Bank");
		assertThat(accounts).hasSize(2);
		AccountResponse checking = accounts.get(0);
		assertThat(checking.institutionName()).isEqualTo("First Platypus Bank");
		assertThat(checking.type()).isEqualTo("depository");
		assertThat(checking.subtype()).isEqualTo("checking");
		assertThat(checking.currentBalance()).isEqualTo(new BigDecimal("110.0"));
		assertThat(checking.availableBalance()).isEqualTo(new BigDecimal("100.1"));
		assertThat(checking.isoCurrencyCode()).isEqualTo("USD");
		assertThat(accounts.get(1).subtype()).isNull();
		assertThat(accounts.get(1).availableBalance()).isNull();
	}

	@Test
	void linkItemRefreshesBalancesOfAnAccountAlreadySaved() throws IOException {
		User user = new User("ada@example.com", "hash");
		Account existing = new Account(user, new PlaidItem(user, "item-0", "old", "First Platypus Bank"),
				"acc-checking", "Plaid Checking", "0000", "depository", "checking");
		existing.updateBalances(new BigDecimal("5"), new BigDecimal("5"), "USD");
		givenPlaidReturns(plaidAccount("acc-checking", AccountType.DEPOSITORY, AccountSubtype.CHECKING, 110.0, 100.0));
		given(userRepository.getReferenceById(42L)).willReturn(user);
		given(plaidItemRepository.save(any())).willAnswer((invocation) -> invocation.getArgument(0));
		given(accountRepository.findByPlaidAccountId("acc-checking")).willReturn(Optional.of(existing));
		given(accountRepository.save(existing)).willReturn(existing);

		plaidService.linkItem(42L, "public-sandbox-abc");

		assertThat(existing.getCurrentBalance()).isEqualTo(new BigDecimal("110.0"));
		assertThat(existing.getAvailableBalance()).isEqualTo(new BigDecimal("100.0"));
	}

	@Test
	void linkItemSavesNothingWhenPlaidFails() throws IOException {
		given(plaidApi.itemPublicTokenExchange(any())).willReturn(exchangeCall);
		given(exchangeCall.execute()).willReturn(Response.error(400, ResponseBody.create(
				"{\"error_code\":\"INVALID_PUBLIC_TOKEN\"}", MediaType.get("application/json"))));

		assertThatThrownBy(() -> plaidService.linkItem(42L, "public-sandbox-expired"))
			.isInstanceOf(PlaidException.class)
			.hasMessageContaining("INVALID_PUBLIC_TOKEN");
		then(plaidItemRepository).should(never()).save(any());
		then(accountRepository).should(never()).save(any());
	}

	private void givenPlaidReturns(AccountBase... accounts) throws IOException {
		given(plaidApi.itemPublicTokenExchange(any())).willReturn(exchangeCall);
		given(exchangeCall.execute()).willReturn(Response.success(
				new ItemPublicTokenExchangeResponse().itemId("item-1").accessToken("access-sandbox-xyz")));
		given(plaidApi.accountsBalanceGet(any())).willReturn(balanceCall);
		given(balanceCall.execute()).willReturn(Response.success(new AccountsGetResponse()
			.item(new Item().itemId("item-1").institutionName("First Platypus Bank"))
			.accounts(List.of(accounts))));
	}

	private static AccountBase plaidAccount(String id, AccountType type, AccountSubtype subtype, Double current,
			Double available) {
		return new AccountBase().accountId(id)
			.name("Plaid " + id)
			.mask("0000")
			.type(type)
			.subtype(subtype)
			.balances(new AccountBalance().current(current).available(available).isoCurrencyCode("USD"));
	}

}
