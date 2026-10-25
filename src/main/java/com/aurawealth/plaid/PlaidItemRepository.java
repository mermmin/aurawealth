package com.aurawealth.plaid;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaidItemRepository extends JpaRepository<PlaidItem, Long> {

	Optional<PlaidItem> findByItemId(String itemId);

	List<PlaidItem> findByUserId(Long userId);

}
