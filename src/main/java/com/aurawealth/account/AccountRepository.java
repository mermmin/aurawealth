package com.aurawealth.account;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AccountRepository extends JpaRepository<Account, Long> {

	Optional<Account> findByPlaidAccountId(String plaidAccountId);

	// Fetches each account's Plaid item in the same query: the API shows the bank name and open-in-view is off
	@EntityGraph(attributePaths = "plaidItem")
	List<Account> findByUserIdOrderByNameAsc(Long userId);

	@Query("""
			SELECT new com.aurawealth.account.BalanceByType(a.type, SUM(a.currentBalance))
			FROM Account a
			WHERE a.user.id = :userId
			GROUP BY a.type""")
	List<BalanceByType> sumCurrentBalancesByType(Long userId);

}
