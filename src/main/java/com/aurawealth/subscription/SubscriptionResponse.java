package com.aurawealth.subscription;

import java.math.BigDecimal;
import java.time.LocalDate;

/** {@code monthlyCost} puts weekly and annual charges on the same scale as monthly ones. */
public record SubscriptionResponse(Long id, String merchantName, BigDecimal amount, String currency,
		Frequency frequency, BigDecimal monthlyCost, LocalDate lastChargedOn, LocalDate nextExpectedOn,
		String category, boolean active) {

	public static SubscriptionResponse from(Subscription subscription) {
		return new SubscriptionResponse(subscription.getId(), subscription.getMerchantName(), subscription.getAmount(),
				subscription.getIsoCurrencyCode(), subscription.getFrequency(),
				subscription.getFrequency().monthlyCost(subscription.getAmount()), subscription.getLastChargedOn(),
				subscription.getNextExpectedOn(), subscription.getCategory(), subscription.isActive());
	}

}
