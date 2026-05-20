CREATE TABLE transactions (
    id              UUID PRIMARY KEY,
    owner_id        UUID                     NOT NULL,
    to_iban         VARCHAR(34)              NOT NULL,
    amount          NUMERIC(19, 2)           NOT NULL,
    currency        VARCHAR(3)               NOT NULL,
    status          VARCHAR(20)              NOT NULL,
    failure_reason  VARCHAR(255),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT amount_positive   CHECK (amount > 0),
    CONSTRAINT currency_iso_4217 CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT status_allowed    CHECK (status IN ('COMPLETED', 'FAILED'))
);

CREATE INDEX idx_transactions_owner ON transactions (owner_id);
