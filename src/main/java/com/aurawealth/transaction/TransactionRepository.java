package com.aurawealth.transaction;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

	Optional<Transaction> findByPlaidTransactionId(String plaidTransactionId);

	/** Applies the "removed" list from /transactions/sync in one statement; returns how many rows were deleted. */
	@Modifying
	@Query("DELETE FROM Transaction t WHERE t.plaidTransactionId IN :plaidTransactionIds")
	int deleteByPlaidTransactionIds(Collection<String> plaidTransactionIds);

}
