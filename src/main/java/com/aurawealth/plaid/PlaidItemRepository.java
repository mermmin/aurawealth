package com.aurawealth.plaid;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PlaidItemRepository extends JpaRepository<PlaidItem, Long> {

	Optional<PlaidItem> findByItemId(String itemId);

	List<PlaidItem> findByUserId(Long userId);

	/**
	 * The least recent sync across the user's banks (null with no banks): totals that combine every bank are only
	 * as current as the stalest one.
	 */
	@Query("SELECT MIN(p.lastSyncedAt) FROM PlaidItem p WHERE p.user.id = :userId")
	Instant findOldestSyncedAt(Long userId);

}
