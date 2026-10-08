CREATE TABLE accounts (
    id         UUID PRIMARY KEY,
    owner_name VARCHAR(120) NOT NULL,
    type       VARCHAR(16)  NOT NULL CHECK (type IN ('CUSTOMER', 'SYSTEM')),
    currency   VARCHAR(3)   NOT NULL CHECK (currency IN ('BRL', 'USD', 'EUR')),
    status     VARCHAR(16)  NOT NULL CHECK (status IN ('ACTIVE', 'FROZEN')),
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL,
    version    BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX ix_accounts_created_at ON accounts (created_at DESC);

-- One system account per currency mirrors the money that lives outside the ledger
-- (bank deposits and withdrawals). It is the counterpart of every deposit and withdrawal.
CREATE UNIQUE INDEX ux_accounts_system_currency ON accounts (currency) WHERE type = 'SYSTEM';

INSERT INTO accounts (id, owner_name, type, currency, status, created_at, updated_at)
SELECT gen_random_uuid(), 'External funds (' || code || ')', 'SYSTEM', code, 'ACTIVE', NOW(), NOW()
FROM (VALUES ('BRL'), ('USD'), ('EUR')) AS currencies (code);
