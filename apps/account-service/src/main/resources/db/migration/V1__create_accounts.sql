CREATE TABLE accounts (
    id          UUID PRIMARY KEY,
    owner_name  VARCHAR(255)              NOT NULL,
    balance     NUMERIC(19, 2)            NOT NULL,
    version     BIGINT                    NOT NULL DEFAULT 0,
    created_at  TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT balance_non_negative CHECK (balance >= 0)
);

CREATE INDEX idx_accounts_owner_name ON accounts(owner_name);
