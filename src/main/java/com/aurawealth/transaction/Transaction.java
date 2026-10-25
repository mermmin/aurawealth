package com.aurawealth.transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.aurawealth.account.Account;
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

/**
 * A transaction synced from Plaid. Amounts keep Plaid's sign convention: positive is money out, negative is money
 * in. Everything except the identity can change when Plaid reports it as modified, e.g. when a pending charge posts.
 */
@Entity
@Table(name = "transactions")
public class Transaction {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, updatable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "account_id", nullable = false, updatable = false)
	private Account account;

	@Column(name = "plaid_transaction_id", nullable = false, unique = true, updatable = false)
	private String plaidTransactionId;

	@Column(nullable = false, precision = 19, scale = 4)
	private BigDecimal amount;

	@Column(name = "iso_currency_code", length = 3)
	private String isoCurrencyCode;

	@Column(name = "transaction_date", nullable = false)
	private LocalDate date;

	@Column(nullable = false)
	private String name;

	@Column(name = "merchant_name")
	private String merchantName;

	private String category;

	@Column(nullable = false)
	private boolean pending;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Transaction() {
		// required by JPA
	}

	public Transaction(User user, Account account, String plaidTransactionId, BigDecimal amount,
			String isoCurrencyCode, LocalDate date, String name, String merchantName, String category,
			boolean pending) {
		this.user = user;
		this.account = account;
		this.plaidTransactionId = plaidTransactionId;
		update(amount, isoCurrencyCode, date, name, merchantName, category, pending);
	}

	/** Applies Plaid's latest version of this transaction (sync reports it as "modified"). */
	public void update(BigDecimal amount, String isoCurrencyCode, LocalDate date, String name, String merchantName,
			String category, boolean pending) {
		this.amount = amount;
		this.isoCurrencyCode = isoCurrencyCode;
		this.date = date;
		this.name = name;
		this.merchantName = merchantName;
		this.category = category;
		this.pending = pending;
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public Account getAccount() {
		return account;
	}

	public String getPlaidTransactionId() {
		return plaidTransactionId;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public String getIsoCurrencyCode() {
		return isoCurrencyCode;
	}

	public LocalDate getDate() {
		return date;
	}

	public String getName() {
		return name;
	}

	public String getMerchantName() {
		return merchantName;
	}

	public String getCategory() {
		return category;
	}

	public boolean isPending() {
		return pending;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
