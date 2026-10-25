package com.aurawealth.transaction;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
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

	/**
	 * Net spending per category between two dates inclusive, biggest first. Refunds (negative amounts) offset
	 * purchases in the same category, and categories that net to zero or less are left out.
	 */
	@Query("""
			SELECT new com.aurawealth.transaction.CategorySpending(t.category, SUM(t.amount))
			FROM Transaction t
			WHERE t.user.id = :userId
				AND t.date BETWEEN :from AND :to
				AND (t.category IS NULL OR t.category NOT IN :excludedCategories)
			GROUP BY t.category
			HAVING SUM(t.amount) > 0
			ORDER BY SUM(t.amount) DESC""")
	List<CategorySpending> sumSpendingByCategory(Long userId, LocalDate from, LocalDate to,
			Collection<String> excludedCategories);

}
