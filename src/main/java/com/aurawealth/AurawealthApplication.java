package com.aurawealth;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AurawealthApplication {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(AurawealthApplication.class);
		application.addListeners(new ProductionSettingsCheck());
		application.run(args);
	}

	/** "Today" for date-window features; tests swap in a fixed clock. */
	@Bean
	public Clock clock() {
		return Clock.systemDefaultZone();
	}

}
