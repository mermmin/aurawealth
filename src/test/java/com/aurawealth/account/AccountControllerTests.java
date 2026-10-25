package com.aurawealth.account;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AccountService accountService;

	@Test
	void listsSignedInUsersAccounts() throws Exception {
		given(accountService.listForUser(42L)).willReturn(List.of(new AccountResponse(7L, "First Platypus Bank",
				"Plaid Checking", "0000", "depository", "checking", new BigDecimal("110.00"), null, "USD",
				Instant.parse("2026-10-25T12:00:00Z"))));

		mockMvc.perform(get("/api/accounts").with(jwt().jwt((token) -> token.subject("42"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].name").value("Plaid Checking"))
			.andExpect(jsonPath("$[0].institutionName").value("First Platypus Bank"))
			.andExpect(jsonPath("$[0].availableBalance").isEmpty());
	}

	@Test
	void requiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/accounts")).andExpect(status().isUnauthorized());
	}

}
