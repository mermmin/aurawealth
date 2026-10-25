package com.aurawealth.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

// Full app against the real Postgres from docker-compose; each test rolls back.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void registerReturnsTokenThatUnlocksMe() throws Exception {
		String token = accessToken(register("Ada@Example.com", "s3cret-pass")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.expiresAt").exists()));

		mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value("ada@example.com"))
			.andExpect(jsonPath("$.id").isNumber());
	}

	@Test
	void registerRejectsDuplicateEmail() throws Exception {
		register("ada@example.com", "s3cret-pass").andExpect(status().isCreated());

		register("ADA@example.com", "another-pass")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Email is already registered"));
	}

	@Test
	void registerRejectsInvalidInput() throws Exception {
		register("not-an-email", "s3cret-pass").andExpect(status().isBadRequest());
		register("ada@example.com", "short").andExpect(status().isBadRequest());
	}

	@Test
	void loginWithCorrectPasswordReturnsToken() throws Exception {
		register("ada@example.com", "s3cret-pass").andExpect(status().isCreated());

		String token = accessToken(login("ada@example.com", "s3cret-pass").andExpect(status().isOk()));

		mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isOk());
	}

	@Test
	void loginRejectsWrongPasswordAndUnknownEmail() throws Exception {
		register("ada@example.com", "s3cret-pass").andExpect(status().isCreated());

		login("ada@example.com", "wrong-pass")
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.detail").value("Invalid email or password"));
		login("nobody@example.com", "s3cret-pass").andExpect(status().isUnauthorized());
	}

	@Test
	void meRequiresValidToken() throws Exception {
		mockMvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-token"))
			.andExpect(status().isUnauthorized());
	}

	private ResultActions register(String email, String password) throws Exception {
		return postJson("/api/auth/register", email, password);
	}

	private ResultActions login(String email, String password) throws Exception {
		return postJson("/api/auth/login", email, password);
	}

	private ResultActions postJson(String path, String email, String password) throws Exception {
		String body = """
				{"email": "%s", "password": "%s"}
				""".formatted(email, password);
		return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private static String accessToken(ResultActions result) throws Exception {
		return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.accessToken");
	}

}
