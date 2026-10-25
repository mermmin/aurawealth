-- Optional name for greetings and the avatar ("Welcome back, Jamie"); the UI falls back to the email when null.
ALTER TABLE users ADD COLUMN display_name VARCHAR(100);

-- When balances were last fetched from Plaid for this bank; drives the dashboard's "As of" date.
ALTER TABLE plaid_items ADD COLUMN last_synced_at TIMESTAMPTZ;

-- Linking already fetches balances, so existing items were last synced when they were created
UPDATE plaid_items SET last_synced_at = created_at;
