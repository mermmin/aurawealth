-- A Plaid Item is one user's login at one bank; it owns the access token used for all later Plaid calls.
-- access_token holds AES-GCM ciphertext (see EncryptedStringConverter), never the raw token, hence TEXT.
CREATE TABLE plaid_items (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id          BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    item_id          VARCHAR(255) NOT NULL UNIQUE,
    access_token     TEXT         NOT NULL,
    institution_name VARCHAR(255),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_plaid_items_user_id ON plaid_items (user_id);

-- Bank accounts under an Item. Balances are refreshed from Plaid, so they can be null when the bank doesn't report one.
CREATE TABLE accounts (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id           BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    plaid_item_id     BIGINT        NOT NULL REFERENCES plaid_items (id) ON DELETE CASCADE,
    plaid_account_id  VARCHAR(255)  NOT NULL UNIQUE,
    name              VARCHAR(255)  NOT NULL,
    mask              VARCHAR(10),
    type              VARCHAR(50)   NOT NULL,
    subtype           VARCHAR(50),
    current_balance   NUMERIC(19,4),
    available_balance NUMERIC(19,4),
    iso_currency_code VARCHAR(3),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_accounts_user_id ON accounts (user_id);
CREATE INDEX idx_accounts_plaid_item_id ON accounts (plaid_item_id);
