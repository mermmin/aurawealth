-- Position in Plaid's /transactions/sync change feed; null until the first sync, then only changes are fetched.
ALTER TABLE plaid_items ADD COLUMN transactions_cursor TEXT;

-- Transactions synced from Plaid. Amounts keep Plaid's sign: positive = money out (purchase), negative = money in.
-- category is Plaid's personal_finance_category.primary (e.g. FOOD_AND_DRINK), used for the spending breakdown.
CREATE TABLE transactions (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id              BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    account_id           BIGINT        NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
    plaid_transaction_id VARCHAR(255)  NOT NULL UNIQUE,
    amount               NUMERIC(19,4) NOT NULL,
    iso_currency_code    VARCHAR(3),
    transaction_date     DATE          NOT NULL,
    name                 VARCHAR(255)  NOT NULL,
    merchant_name        VARCHAR(255),
    category             VARCHAR(100),
    pending              BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- Dashboard queries filter one user's transactions by date range (spending in the last 30 days)
CREATE INDEX idx_transactions_user_id_date ON transactions (user_id, transaction_date);
CREATE INDEX idx_transactions_account_id ON transactions (account_id);
