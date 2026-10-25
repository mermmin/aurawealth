package com.aurawealth.dashboard;

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

import com.aurawealth.transaction.CategorySpending;

@SpringBootTest
@AutoConfigureMockMvc
class DashboardControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private DashboardService dashboardService;

	@Test
	void netWorthForSignedInUser() throws Exception {
		given(dashboardService.netWorth(42L)).willReturn(new NetWorthResponse(new BigDecimal("1000.00"),
				new BigDecimal("250.00"), new BigDecimal("750.00"), "USD"));

		mockMvc.perform(get("/api/dashboard/net-worth").with(jwt().jwt((token) -> token.subject("42"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.assets").value(1000.00))
			.andExpect(jsonPath("$.liabilities").value(250.00))
			.andExpect(jsonPath("$.netWorth").value(750.00))
			.andExpect(jsonPath("$.currency").value("USD"));
	}

	@Test
	void netWorthRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/dashboard/net-worth")).andExpect(status().isUnauthorized());
	}

	@Test
	void spendingForSignedInUser() throws Exception {
		given(dashboardService.spending(42L)).willReturn(new SpendingResponse(LocalDate.of(2026, 9, 26),
				LocalDate.of(2026, 10, 25), new BigDecimal("160.50"), "USD",
				List.of(new CategorySpending("FOOD_AND_DRINK", new BigDecimal("120.50")),
						new CategorySpending("TRANSPORTATION", new BigDecimal("40.00")))));

		mockMvc.perform(get("/api/dashboard/spending").with(jwt().jwt((token) -> token.subject("42"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.from").value("2026-09-26"))
			.andExpect(jsonPath("$.to").value("2026-10-25"))
			.andExpect(jsonPath("$.total").value(160.50))
			.andExpect(jsonPath("$.categories[0].category").value("FOOD_AND_DRINK"))
			.andExpect(jsonPath("$.categories[0].amount").value(120.50))
			.andExpect(jsonPath("$.categories.length()").value(2));
	}

	@Test
	void spendingRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/dashboard/spending")).andExpect(status().isUnauthorized());
	}

}
