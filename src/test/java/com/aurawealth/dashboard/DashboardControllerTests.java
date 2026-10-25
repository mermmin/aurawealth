package com.aurawealth.dashboard;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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

}
