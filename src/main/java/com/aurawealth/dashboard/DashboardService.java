package com.aurawealth.dashboard;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.aurawealth.account.AccountRepository;
import com.aurawealth.account.BalanceByType;

@Service
public class DashboardService {

	// v1 links US sandbox banks only (see PlaidService.createLinkToken), so every balance is in dollars
	static final String CURRENCY = "USD";

	// Plaid reports these balances as positive amounts owed; every other type (depository, investment, ...) is held
	private static final Set<String> LIABILITY_TYPES = Set.of("credit", "loan");

	private final AccountRepository accountRepository;

	public DashboardService(AccountRepository accountRepository) {
		this.accountRepository = accountRepository;
	}

	public NetWorthResponse netWorth(Long userId) {
		BigDecimal assets = BigDecimal.ZERO;
		BigDecimal liabilities = BigDecimal.ZERO;
		for (BalanceByType row : accountRepository.sumCurrentBalancesByType(userId)) {
			BigDecimal total = Objects.requireNonNullElse(row.total(), BigDecimal.ZERO);
			if (LIABILITY_TYPES.contains(row.type())) {
				liabilities = liabilities.add(total);
			}
			else {
				assets = assets.add(total);
			}
		}
		return new NetWorthResponse(assets, liabilities, assets.subtract(liabilities), CURRENCY);
	}

}
