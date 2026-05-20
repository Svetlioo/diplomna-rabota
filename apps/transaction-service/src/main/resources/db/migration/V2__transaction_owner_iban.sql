-- A transaction now records who initiated it (owner_id, from the JWT) and the
-- destination IBAN, instead of opaque from/to account ids.
DROP INDEX idx_transactions_from;
DROP INDEX idx_transactions_to;
ALTER TABLE transactions DROP CONSTRAINT accounts_differ;

ALTER TABLE transactions DROP COLUMN from_account_id;
ALTER TABLE transactions DROP COLUMN to_account_id;

ALTER TABLE transactions ADD COLUMN owner_id UUID         NOT NULL;
ALTER TABLE transactions ADD COLUMN to_iban  VARCHAR(34)  NOT NULL;

CREATE INDEX idx_transactions_owner ON transactions (owner_id);
