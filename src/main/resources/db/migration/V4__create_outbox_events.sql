CREATE TABLE outbox_events (
    id              UUID PRIMARY KEY,
    aggregate_id    UUID         NOT NULL,
    event_type      VARCHAR(64)  NOT NULL,
    payload         JSONB        NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    published_at    TIMESTAMPTZ,
    attempts        INT          NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ  NOT NULL,
    last_error      VARCHAR(300)
);

CREATE INDEX ix_outbox_events_pending ON outbox_events (next_attempt_at) WHERE published_at IS NULL;
