package com.aurawealth;

import java.util.List;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;

/**
 * With the {@code prod} profile, refuses to start unless every deploy variable is set. Runs before any bean is
 * created, so the error names what's missing instead of failing later on a half-resolved {@code ${DB_URL}}.
 */
class ProductionSettingsCheck implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

	static final List<String> REQUIRED = List.of("DB_URL", "DB_USERNAME", "DB_PASSWORD", "JWT_SECRET",
			"TOKEN_ENCRYPTION_KEY", "PLAID_CLIENT_ID", "PLAID_SECRET");

	@Override
	public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
		check(event.getEnvironment());
	}

	static void check(ConfigurableEnvironment environment) {
		if (!environment.matchesProfiles("prod")) {
			return;
		}
		List<String> missing = REQUIRED.stream().filter((name) -> !StringUtils.hasText(environment.getProperty(name))).toList();
		if (!missing.isEmpty()) {
			throw new IllegalStateException("Missing production settings: " + String.join(", ", missing));
		}
	}

}
