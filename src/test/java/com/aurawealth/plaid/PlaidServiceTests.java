package com.aurawealth.plaid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.plaid.client.model.CountryCode;
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
	private Call<LinkTokenCreateResponse> call;

	@Captor
	private ArgumentCaptor<LinkTokenCreateRequest> request;

	@InjectMocks
	private PlaidService plaidService;

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

}
