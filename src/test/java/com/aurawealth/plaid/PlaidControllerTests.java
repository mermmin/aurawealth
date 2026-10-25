package com.aurawealth.plaid;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.aurawealth.account.AccountResponse;

@SpringBootTest
@AutoConfigureMockMvc
class PlaidControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PlaidService plaidService;

	@Test
	void linkTokenIsCreatedForSignedInUser() throws Exception {
		given(plaidService.createLinkToken(42L)).willReturn(
				new LinkTokenResponse("link-sandbox-123", OffsetDateTime.parse("2026-10-25T06:00:00Z")));

		mockMvc.perform(post("/api/plaid/link-token").with(jwt().jwt((token) -> token.subject("42"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.linkToken").value("link-sandbox-123"));
	}

	@Test
	void linkTokenRequiresAuthentication() throws Exception {
		mockMvc.perform(post("/api/plaid/link-token")).andExpect(status().isUnauthorized());
	}

	@Test
	void plaidFailureReturnsBadGateway() throws Exception {
		given(plaidService.createLinkToken(42L)).willThrow(new PlaidException("Plaid request failed"));

		mockMvc.perform(post("/api/plaid/link-token").with(jwt().jwt((token) -> token.subject("42"))))
			.andExpect(status().isBadGateway())
			.andExpect(jsonPath("$.detail").value("Bank connection service is unavailable"));
	}

	@Test
	void exchangeLinksItemAndReturnsAccounts() throws Exception {
		given(plaidService.linkItem(42L, "public-sandbox-abc")).willReturn(List.of(new AccountResponse(7L,
				"First Platypus Bank", "Plaid Checking", "0000", "depository", "checking", new BigDecimal("110.00"),
				new BigDecimal("100.00"), "USD", Instant.parse("2026-10-25T12:00:00Z"))));

		mockMvc
			.perform(post("/api/plaid/exchange").with(jwt().jwt((token) -> token.subject("42")))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"publicToken\":\"public-sandbox-abc\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$[0].institutionName").value("First Platypus Bank"))
			.andExpect(jsonPath("$[0].currentBalance").value(110.00))
			.andExpect(jsonPath("$[0].accessToken").doesNotExist());
	}

	@Test
	void exchangeRejectsBlankPublicToken() throws Exception {
		mockMvc
			.perform(post("/api/plaid/exchange").with(jwt().jwt((token) -> token.subject("42")))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"publicToken\":\" \"}"))
			.andExpect(status().isBadRequest());
		then(plaidService).should(never()).linkItem(any(), any());
	}

	@Test
	void exchangeRequiresAuthentication() throws Exception {
		mockMvc
			.perform(post("/api/plaid/exchange").contentType(MediaType.APPLICATION_JSON)
				.content("{\"publicToken\":\"public-sandbox-abc\"}"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void syncReturnsWhatChanged() throws Exception {
		given(plaidService.sync(42L)).willReturn(new SyncResponse(14, 48, 2, 1));

		mockMvc.perform(post("/api/plaid/sync").with(jwt().jwt((token) -> token.subject("42"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accounts").value(14))
			.andExpect(jsonPath("$.added").value(48))
			.andExpect(jsonPath("$.modified").value(2))
			.andExpect(jsonPath("$.removed").value(1));
	}

	@Test
	void syncRequiresAuthentication() throws Exception {
		mockMvc.perform(post("/api/plaid/sync")).andExpect(status().isUnauthorized());
		then(plaidService).should(never()).sync(any());
	}

}
