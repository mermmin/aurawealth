-- Recurring charges detected from a user's transactions (RecurringChargeDetector), one row per merchant.
-- merchant_key is the normalized grouping key (trimmed, lowercased); merchant_name is what the UI shows.
CREATE TABLE subscriptions (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id           BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    merchant_key      VARCHAR(255)  NOT NULL,
    merchant_name     VARCHAR(255)  NOT NULL,
    amount            NUMERIC(19,4) NOT NULL,
    iso_currency_code VARCHAR(3),
    frequency         VARCHAR(10)   NOT NULL CHECK (frequency IN ('WEEKLY', 'MONTHLY', 'ANNUAL')),
    last_charged_on   DATE          NOT NULL,
    next_expected_on  DATE          NOT NULL,
    category          VARCHAR(100),
    active            BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    -- Also serves as the index for "this user's subscriptions" (user_id is the leading column)
    UNIQUE (user_id, merchant_key)
);
