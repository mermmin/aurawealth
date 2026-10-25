package com.aurawealth.plaid;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Service;

import com.plaid.client.model.CountryCode;
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

	public PlaidService(PlaidApi plaidApi) {
		this.plaidApi = plaidApi;
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
