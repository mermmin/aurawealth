package com.aurawealth.plaid;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import com.aurawealth.security.EncryptedStringConverter;
import com.aurawealth.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** One user's link to one bank. The access token is encrypted in the database and plain only in memory. */
@Entity
@Table(name = "plaid_items")
public class PlaidItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, updatable = false)
	private User user;

	@Column(name = "item_id", nullable = false, unique = true, updatable = false)
	private String itemId;

	@Convert(converter = EncryptedStringConverter.class)
	@Column(name = "access_token", nullable = false)
	private String accessToken;

	@Column(name = "institution_name")
	private String institutionName;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "transactions_cursor")
	private String transactionsCursor;

	@Column(name = "last_synced_at")
	private Instant lastSyncedAt;

	protected PlaidItem() {
		// required by JPA
	}

	public PlaidItem(User user, String itemId, String accessToken, String institutionName) {
		this.user = user;
		this.itemId = itemId;
		this.accessToken = accessToken;
		this.institutionName = institutionName;
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public String getItemId() {
		return itemId;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public String getInstitutionName() {
		return institutionName;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	/** Where the last /transactions/sync left off; null means the next sync starts from the beginning. */
	public String getTransactionsCursor() {
		return transactionsCursor;
	}

	public void updateTransactionsCursor(String transactionsCursor) {
		this.transactionsCursor = transactionsCursor;
	}

	/** When balances were last fetched from Plaid; set on link and on every sync. */
	public Instant getLastSyncedAt() {
		return lastSyncedAt;
	}

	public void markSynced(Instant syncedAt) {
		this.lastSyncedAt = syncedAt;
	}

	@Override
	public String toString() {
		// Keep the access token out of logs
		return "PlaidItem[id=" + id + ", itemId=" + itemId + "]";
	}

}
