package com.aurawealth.user;

import java.time.Instant;
import java.util.Locale;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Column(name = "display_name", length = 100)
	private String displayName;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected User() {
		// required by JPA
	}

	public User(String email, String passwordHash) {
		this(email, passwordHash, null);
	}

	/** A blank display name is stored as null, so the UI's fallback to the email applies. */
	public User(String email, String passwordHash, String displayName) {
		this.email = normalizeEmail(email);
		this.passwordHash = passwordHash;
		this.displayName = (displayName == null || displayName.isBlank()) ? null : displayName.trim();
	}

	/** Emails are stored lowercased and trimmed so the unique constraint is effectively case-insensitive. */
	public static String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	public Long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public String getDisplayName() {
		return displayName;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
