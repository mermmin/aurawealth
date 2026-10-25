package com.aurawealth.account;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.UpdateTimestamp;

import com.aurawealth.plaid.PlaidItem;
import com.aurawealth.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A bank account synced from Plaid. Identity fields are fixed; name and balances are refreshed on each sync. */
@Entity
@Table(name = "accounts")
public class Account {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, updatable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "plaid_item_id", nullable = false, updatable = false)
	private PlaidItem plaidItem;

	@Column(name = "plaid_account_id", nullable = false, unique = true, updatable = false)
	private String plaidAccountId;

	@Column(nullable = false)
	private String name;

	private String mask;

	@Column(nullable = false)
	private String type;

	private String subtype;

	@Column(name = "current_balance", precision = 19, scale = 4)
	private BigDecimal currentBalance;

	@Column(name = "available_balance", precision = 19, scale = 4)
	private BigDecimal availableBalance;

	@Column(name = "iso_currency_code", length = 3)
	private String isoCurrencyCode;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Account() {
		// required by JPA
	}

	public Account(User user, PlaidItem plaidItem, String plaidAccountId, String name, String mask, String type,
			String subtype) {
		this.user = user;
		this.plaidItem = plaidItem;
		this.plaidAccountId = plaidAccountId;
		this.name = name;
		this.mask = mask;
		this.type = type;
		this.subtype = subtype;
	}

	public void updateBalances(BigDecimal currentBalance, BigDecimal availableBalance, String isoCurrencyCode) {
		this.currentBalance = currentBalance;
		this.availableBalance = availableBalance;
		this.isoCurrencyCode = isoCurrencyCode;
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public PlaidItem getPlaidItem() {
		return plaidItem;
	}

	public String getPlaidAccountId() {
		return plaidAccountId;
	}

	public String getName() {
		return name;
	}

	public String getMask() {
		return mask;
	}

	public String getType() {
		return type;
	}

	public String getSubtype() {
		return subtype;
	}

	public BigDecimal getCurrentBalance() {
		return currentBalance;
	}

	public BigDecimal getAvailableBalance() {
		return availableBalance;
	}

	public String getIsoCurrencyCode() {
		return isoCurrencyCode;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
