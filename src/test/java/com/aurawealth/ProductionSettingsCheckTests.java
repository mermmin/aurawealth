package com.aurawealth;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionSettingsCheckTests {

	@Test
	void ignoresLocalDev() {
		assertThatCode(() -> ProductionSettingsCheck.check(new MockEnvironment())).doesNotThrowAnyException();
	}

	@Test
	void namesEveryMissingSettingInProd() {
		MockEnvironment environment = prod();
		environment.setProperty("DB_URL", "jdbc:postgresql://db/aurawealth");
		environment.setProperty("JWT_SECRET", " ");

		assertThatIllegalStateException().isThrownBy(() -> ProductionSettingsCheck.check(environment))
			.withMessage("Missing production settings: DB_USERNAME, DB_PASSWORD, JWT_SECRET, TOKEN_ENCRYPTION_KEY, "
					+ "PLAID_CLIENT_ID, PLAID_SECRET");
	}

	@Test
	void startsInProdWhenEverythingIsSet() {
		MockEnvironment environment = prod();
		ProductionSettingsCheck.REQUIRED.forEach((name) -> environment.setProperty(name, "set"));

		assertThatCode(() -> ProductionSettingsCheck.check(environment)).doesNotThrowAnyException();
	}

	private static MockEnvironment prod() {
		MockEnvironment environment = new MockEnvironment();
		environment.setActiveProfiles("prod");
		return environment;
	}

}
