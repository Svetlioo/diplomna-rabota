CREATE TABLE transactions (
    id              UUID PRIMARY KEY,
    from_account_id UUID                     NOT NULL,
    to_account_id   UUID                     NOT NULL,
    amount          NUMERIC(19, 2)           NOT NULL,
    currency        VARCHAR(3)               NOT NULL,
    status          VARCHAR(20)              NOT NULL,
    failure_reason  VARCHAR(255),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT amount_positive       CHECK (amount > 0),
    CONSTRAINT currency_iso_4217     CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT status_allowed        CHECK (status IN ('COMPLETED', 'FAILED')),
    CONSTRAINT accounts_differ       CHECK (from_account_id <> to_account_id)
);

CREATE INDEX idx_transactions_from ON transactions (from_account_id);
CREATE INDEX idx_transactions_to   ON transactions (to_account_id);
