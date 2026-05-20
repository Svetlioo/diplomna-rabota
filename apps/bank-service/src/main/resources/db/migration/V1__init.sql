CREATE TABLE users (
    id            UUID PRIMARY KEY,
    email         VARCHAR(255)             NOT NULL UNIQUE,
    password_hash VARCHAR(255)             NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE accounts (
    id          UUID PRIMARY KEY,
    owner_id    UUID                     NOT NULL UNIQUE REFERENCES users (id),
    iban        VARCHAR(34)              NOT NULL UNIQUE,
    balance     NUMERIC(19, 2)           NOT NULL,
    currency    VARCHAR(3)               NOT NULL,
    version     BIGINT                   NOT NULL DEFAULT 0,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT balance_non_negative CHECK (balance >= 0),
    CONSTRAINT currency_iso_4217   CHECK (currency ~ '^[A-Z]{3}$')
);

CREATE TABLE transactions (
    id          UUID PRIMARY KEY,
    owner_id    UUID                     NOT NULL,
    to_iban     VARCHAR(34)              NOT NULL,
    amount      NUMERIC(19, 2)           NOT NULL,
    currency    VARCHAR(3)               NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT amount_positive       CHECK (amount > 0),
    CONSTRAINT currency_iso_4217_tx  CHECK (currency ~ '^[A-Z]{3}$')
);

CREATE INDEX idx_transactions_owner ON transactions (owner_id);
