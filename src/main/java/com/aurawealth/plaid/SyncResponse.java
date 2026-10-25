package com.aurawealth.plaid;

/** What one sync changed, summed over all of the user's linked banks. */
public record SyncResponse(int accounts, int added, int modified, int removed) {

	static final SyncResponse NONE = new SyncResponse(0, 0, 0, 0);

	SyncResponse plus(SyncResponse other) {
		return new SyncResponse(accounts + other.accounts, added + other.added, modified + other.modified,
				removed + other.removed);
	}

}
