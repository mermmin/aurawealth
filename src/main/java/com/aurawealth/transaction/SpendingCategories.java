package com.aurawealth.transaction;

import java.util.Set;

public final class SpendingCategories {

	/**
	 * Plaid categories that aren't spending: money moving between the user's own accounts (counting a credit card
	 * payment would double-count the purchases already made on that card) and money coming in.
	 */
	public static final Set<String> NON_SPENDING = Set.of("TRANSFER_IN", "TRANSFER_OUT", "LOAN_PAYMENTS", "INCOME");

	private SpendingCategories() {
	}

}
