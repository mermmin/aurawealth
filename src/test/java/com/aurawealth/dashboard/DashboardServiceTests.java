package com.aurawealth.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.aurawealth.account.AccountRepository;
import com.aurawealth.account.BalanceByType;
import com.aurawealth.transaction.CategorySpending;
import com.aurawealth.transaction.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTests {

	private static final Clock OCT_25 = Clock.fixed(Instant.parse("2026-10-25T12:00:00Z"), ZoneOffset.UTC);

	@Mock
	private AccountRepository accountRepository;

	@Mock
	private TransactionRepository transactionRepository;

	private DashboardService dashboardService;

	@BeforeEach
	void setUp() {
		dashboardService = new DashboardService(accountRepository, transactionRepository, OCT_25);
	}

	@Test
	void netWorthIsAssetsMinusCreditAndLoans() {
		given(accountRepository.sumCurrentBalancesByType(42L)).willReturn(List.of(
				new BalanceByType("depository", new BigDecimal("62589.00")),
				new BalanceByType("investment", new BigDecimal("23952.7405")),
				new BalanceByType("credit", new BigDecimal("5430.00")),
				new BalanceByType("loan", new BigDecimal("158275.89")),
				new BalanceByType("other", new BigDecimal("100"))));

		NetWorthResponse netWorth = dashboardService.netWorth(42L);

		assertThat(netWorth.assets()).isEqualByComparingTo("86641.7405");
		assertThat(netWorth.liabilities()).isEqualByComparingTo("163705.89");
		assertThat(netWorth.netWorth()).isEqualByComparingTo("-77064.1495");
		assertThat(netWorth.currency()).isEqualTo("USD");
	}

	@Test
	void typesWithNoBalancesCountAsZero() {
		given(accountRepository.sumCurrentBalancesByType(42L)).willReturn(List.of(
				new BalanceByType("depository", new BigDecimal("100")), new BalanceByType("credit", null)));

		NetWorthResponse netWorth = dashboardService.netWorth(42L);

		assertThat(netWorth.liabilities()).isEqualByComparingTo("0");
		assertThat(netWorth.netWorth()).isEqualByComparingTo("100");
	}

	@Test
	void noAccountsMeansZeroNetWorth() {
		given(accountRepository.sumCurrentBalancesByType(42L)).willReturn(List.of());

		assertThat(dashboardService.netWorth(42L))
			.isEqualTo(new NetWorthResponse(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "USD"));
	}

	@Test
	void spendingCoversTodayAndThe29DaysBeforeAndExcludesNonSpending() {
		given(transactionRepository.sumSpendingByCategory(42L, LocalDate.of(2026, 9, 26), LocalDate.of(2026, 10, 25),
				DashboardService.NON_SPENDING_CATEGORIES))
			.willReturn(List.of(new CategorySpending("FOOD_AND_DRINK", new BigDecimal("120.50")),
					new CategorySpending("TRANSPORTATION", new BigDecimal("40.00")),
					new CategorySpending(null, new BigDecimal("7.25"))));

		SpendingResponse spending = dashboardService.spending(42L);

		assertThat(spending.from()).isEqualTo(LocalDate.of(2026, 9, 26));
		assertThat(spending.to()).isEqualTo(LocalDate.of(2026, 10, 25));
		assertThat(spending.total()).isEqualByComparingTo("167.75");
		assertThat(spending.currency()).isEqualTo("USD");
		assertThat(spending.categories()).extracting(CategorySpending::category)
			.containsExactly("FOOD_AND_DRINK", "TRANSPORTATION", "UNCATEGORIZED");
		assertThat(DashboardService.NON_SPENDING_CATEGORIES).contains("LOAN_PAYMENTS", "TRANSFER_OUT");
	}

	@Test
	void noSpendingMeansEmptyCategoriesAndZeroTotal() {
		given(transactionRepository.sumSpendingByCategory(eq(42L), any(), any(), any())).willReturn(List.of());

		SpendingResponse spending = dashboardService.spending(42L);

		assertThat(spending.categories()).isEmpty();
		assertThat(spending.total()).isEqualByComparingTo("0");
	}

}
