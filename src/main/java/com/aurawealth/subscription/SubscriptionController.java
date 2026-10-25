package com.aurawealth.subscription;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SubscriptionController {

	private final SubscriptionService subscriptionService;

	public SubscriptionController(SubscriptionService subscriptionService) {
		this.subscriptionService = subscriptionService;
	}

	/** The subscriptions ledger; detection itself runs after every bank sync. */
	@GetMapping("/api/subscriptions")
	public List<SubscriptionResponse> list(@AuthenticationPrincipal Jwt jwt) {
		return subscriptionService.listForUser(Long.valueOf(jwt.getSubject()));
	}

}
