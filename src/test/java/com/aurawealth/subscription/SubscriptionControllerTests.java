package com.aurawealth.subscription;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SubscriptionControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SubscriptionService subscriptionService;

	@Test
	void listsSignedInUsersSubscriptions() throws Exception {
		given(subscriptionService.listForUser(42L)).willReturn(List.of(new SubscriptionResponse(3L,
				"Touchstone Climbing", new BigDecimal("78.50"), "USD", Frequency.MONTHLY, new BigDecimal("78.50"),
				LocalDate.of(2026, 10, 9), LocalDate.of(2026, 11, 9), "PERSONAL_CARE", true)));

		mockMvc.perform(get("/api/subscriptions").with(jwt().jwt((token) -> token.subject("42"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].merchantName").value("Touchstone Climbing"))
			.andExpect(jsonPath("$[0].frequency").value("MONTHLY"))
			.andExpect(jsonPath("$[0].monthlyCost").value(78.50))
			.andExpect(jsonPath("$[0].nextExpectedOn").value("2026-11-09"))
			.andExpect(jsonPath("$[0].active").value(true));
	}

	@Test
	void requiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/subscriptions")).andExpect(status().isUnauthorized());
	}

}
