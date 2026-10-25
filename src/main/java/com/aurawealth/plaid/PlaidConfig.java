package com.aurawealth.plaid;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.plaid.client.ApiClient;
import com.plaid.client.request.PlaidApi;

@Configuration
public class PlaidConfig {

	@Bean
	public PlaidApi plaidApi(@Value("${aurawealth.plaid.client-id}") String clientId,
			@Value("${aurawealth.plaid.secret}") String secret) {
		ApiClient apiClient = new ApiClient(Map.of("clientId", clientId, "secret", secret));
		// v1 is sandbox-only: fake banks and test credentials, never real data
		apiClient.setPlaidAdapter(ApiClient.Sandbox);
		return apiClient.createService(PlaidApi.class);
	}

}
