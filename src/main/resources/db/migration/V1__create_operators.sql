CREATE TABLE operators (
    id            UUID PRIMARY KEY,
    email         VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL
);

CREATE UNIQUE INDEX ux_operators_email ON operators (LOWER(email));
