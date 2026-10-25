package com.aurawealth.subscription;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.aurawealth.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A recurring charge detected from the user's transactions; refreshed after every sync. */
@Entity
@Table(name = "subscriptions")
public class Subscription {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, updatable = false)
	private User user;

	@Column(name = "merchant_key", nullable = false, updatable = false)
	private String merchantKey;

	@Column(name = "merchant_name", nullable = false)
	private String merchantName;

	@Column(nullable = false, precision = 19, scale = 4)
	private BigDecimal amount;

	@Column(name = "iso_currency_code", length = 3)
	private String isoCurrencyCode;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private Frequency frequency;

	@Column(name = "last_charged_on", nullable = false)
	private LocalDate lastChargedOn;

	@Column(name = "next_expected_on", nullable = false)
	private LocalDate nextExpectedOn;

	private String category;

	@Column(nullable = false)
	private boolean active;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Subscription() {
		// required by JPA
	}

	public Subscription(User user, String merchantKey, String merchantName, BigDecimal amount,
			String isoCurrencyCode, Frequency frequency, LocalDate lastChargedOn, String category, boolean active) {
		this.user = user;
		this.merchantKey = merchantKey;
		update(merchantName, amount, isoCurrencyCode, frequency, lastChargedOn, category, active);
	}

	/** Applies the latest detection; the next charge is expected one period after the last one. */
	public void update(String merchantName, BigDecimal amount, String isoCurrencyCode, Frequency frequency,
			LocalDate lastChargedOn, String category, boolean active) {
		this.merchantName = merchantName;
		this.amount = amount;
		this.isoCurrencyCode = isoCurrencyCode;
		this.frequency = frequency;
		this.lastChargedOn = lastChargedOn;
		this.nextExpectedOn = lastChargedOn.plus(frequency.period());
		this.category = category;
		this.active = active;
	}

	/** For subscriptions the detector no longer finds, e.g. after Plaid removed their transactions. */
	public void deactivate() {
		this.active = false;
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public String getMerchantKey() {
		return merchantKey;
	}

	public String getMerchantName() {
		return merchantName;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public String getIsoCurrencyCode() {
		return isoCurrencyCode;
	}

	public Frequency getFrequency() {
		return frequency;
	}

	public LocalDate getLastChargedOn() {
		return lastChargedOn;
	}

	public LocalDate getNextExpectedOn() {
		return nextExpectedOn;
	}

	public String getCategory() {
		return category;
	}

	public boolean isActive() {
		return active;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
