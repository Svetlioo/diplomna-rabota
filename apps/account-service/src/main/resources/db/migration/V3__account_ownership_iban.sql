-- An account now belongs to a registered user (one account per user) and is
-- addressed by its IBAN. The free-text owner_name is replaced by owner_id.
ALTER TABLE accounts DROP COLUMN owner_name;

ALTER TABLE accounts ADD COLUMN owner_id UUID NOT NULL REFERENCES users (id);
ALTER TABLE accounts ADD COLUMN iban VARCHAR(34) NOT NULL;

ALTER TABLE accounts ADD CONSTRAINT accounts_owner_id_key UNIQUE (owner_id);
ALTER TABLE accounts ADD CONSTRAINT accounts_iban_key UNIQUE (iban);
