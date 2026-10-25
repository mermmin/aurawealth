package com.aurawealth.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.aurawealth.account.AccountRepository;
import com.aurawealth.account.BalanceByType;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTests {

	@Mock
	private AccountRepository accountRepository;

	private DashboardService dashboardService;

	@BeforeEach
	void setUp() {
		dashboardService = new DashboardService(accountRepository);
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

}
