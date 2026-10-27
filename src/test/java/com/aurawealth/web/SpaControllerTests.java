package com.aurawealth.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SpaControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void appRoutesForwardToTheAppWithoutSigningIn() throws Exception {
		for (String route : new String[] { "/dashboard", "/accounts", "/subscriptions", "/login" }) {
			mockMvc.perform(get(route)).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
		}
	}

	@Test
	void apiStillRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/accounts")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api")).andExpect(status().isUnauthorized());
	}

	@Test
	void filesAreNotForwarded() throws Exception {
		mockMvc.perform(get("/missing.js")).andExpect(status().isNotFound());
	}

}
