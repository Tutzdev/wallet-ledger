CREATE TABLE transfers (
    id                     UUID PRIMARY KEY,
    type                   VARCHAR(16) NOT NULL CHECK (type IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER', 'REVERSAL')),
    source_account_id      UUID        NOT NULL REFERENCES accounts (id),
    destination_account_id UUID        NOT NULL REFERENCES accounts (id),
    amount_minor           BIGINT      NOT NULL CHECK (amount_minor > 0),
    currency               VARCHAR(3)  NOT NULL,
    description            VARCHAR(140),
    reversal_of_id         UUID REFERENCES transfers (id),
    idempotency_key        VARCHAR(64) NOT NULL,
    request_fingerprint    VARCHAR(64) NOT NULL,
    created_by             UUID        NOT NULL REFERENCES operators (id),
    created_at             TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_transfers_idempotency_key UNIQUE (created_by, idempotency_key),
    CONSTRAINT uk_transfers_reversal_of UNIQUE (reversal_of_id),
    CONSTRAINT ck_transfers_distinct_accounts CHECK (source_account_id <> destination_account_id)
);

CREATE INDEX ix_transfers_created_at ON transfers (created_at DESC);
CREATE INDEX ix_transfers_source ON transfers (source_account_id, created_at DESC);
CREATE INDEX ix_transfers_destination ON transfers (destination_account_id, created_at DESC);

CREATE TABLE ledger_entries (
    id           UUID PRIMARY KEY,
    transfer_id  UUID        NOT NULL REFERENCES transfers (id),
    account_id   UUID        NOT NULL REFERENCES accounts (id),
    amount_minor BIGINT      NOT NULL CHECK (amount_minor <> 0),
    currency     VARCHAR(3)  NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_ledger_entries_account ON ledger_entries (account_id, created_at DESC);
CREATE INDEX ix_ledger_entries_transfer ON ledger_entries (transfer_id);

-- Money history is append-only: a mistake is fixed with a reversal, never with UPDATE or DELETE.
CREATE FUNCTION reject_ledger_mutation() RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION '% is append-only', TG_TABLE_NAME;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER transfers_append_only
    BEFORE UPDATE OR DELETE ON transfers
    FOR EACH ROW EXECUTE FUNCTION reject_ledger_mutation();

CREATE TRIGGER ledger_entries_append_only
    BEFORE UPDATE OR DELETE ON ledger_entries
    FOR EACH ROW EXECUTE FUNCTION reject_ledger_mutation();
