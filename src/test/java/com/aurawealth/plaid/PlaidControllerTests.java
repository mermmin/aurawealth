package com.aurawealth.plaid;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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

}
