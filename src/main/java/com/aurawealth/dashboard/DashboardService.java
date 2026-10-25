package com.aurawealth.dashboard;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.aurawealth.account.AccountRepository;
import com.aurawealth.account.BalanceByType;
import com.aurawealth.plaid.PlaidItemRepository;
import com.aurawealth.transaction.CategorySpending;
import com.aurawealth.transaction.SpendingCategories;
import com.aurawealth.transaction.TransactionRepository;

@Service
public class DashboardService {

	// v1 links US sandbox banks only (see PlaidService.createLinkToken), so every balance is in dollars
	static final String CURRENCY = "USD";

	static final int SPENDING_WINDOW_DAYS = 30;

	static final String UNCATEGORIZED = "UNCATEGORIZED";

	// Plaid reports these balances as positive amounts owed; every other type (depository, investment, ...) is held
	private static final Set<String> LIABILITY_TYPES = Set.of("credit", "loan");

	private final AccountRepository accountRepository;

	private final TransactionRepository transactionRepository;

	private final PlaidItemRepository plaidItemRepository;

	private final Clock clock;

	public DashboardService(AccountRepository accountRepository, TransactionRepository transactionRepository,
			PlaidItemRepository plaidItemRepository, Clock clock) {
		this.accountRepository = accountRepository;
		this.transactionRepository = transactionRepository;
		this.plaidItemRepository = plaidItemRepository;
		this.clock = clock;
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
		return new NetWorthResponse(assets, liabilities, assets.subtract(liabilities), CURRENCY,
				plaidItemRepository.findOldestSyncedAt(userId));
	}

	/** Spending by category over the last 30 days: today and the 29 days before it. */
	public SpendingResponse spending(Long userId) {
		LocalDate to = LocalDate.now(clock);
		LocalDate from = to.minusDays(SPENDING_WINDOW_DAYS - 1);
		List<CategorySpending> categories = transactionRepository
			.sumSpendingByCategory(userId, from, to, SpendingCategories.NON_SPENDING)
			.stream()
			.map((row) -> new CategorySpending(Objects.requireNonNullElse(row.category(), UNCATEGORIZED),
					row.amount()))
			.toList();
		BigDecimal total = categories.stream().map(CategorySpending::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
		return new SpendingResponse(from, to, total, CURRENCY, categories);
	}

}
