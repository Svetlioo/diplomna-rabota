CREATE TABLE accounts (
    id          UUID PRIMARY KEY,
    owner_name  VARCHAR(255)              NOT NULL,
    balance     NUMERIC(19, 2)            NOT NULL,
    currency    VARCHAR(3)                NOT NULL,
    version     BIGINT                    NOT NULL DEFAULT 0,
    created_at  TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT balance_non_negative CHECK (balance >= 0),
    CONSTRAINT currency_iso_4217   CHECK (currency ~ '^[A-Z]{3}$')
);
