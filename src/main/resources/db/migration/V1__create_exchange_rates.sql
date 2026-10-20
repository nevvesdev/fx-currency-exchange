CREATE TABLE exchange_rates (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    base_currency  VARCHAR(3)      NOT NULL,
    quote_currency VARCHAR(3)      NOT NULL,
    rate           NUMERIC(40, 20) NOT NULL,
    quoted_at      TIMESTAMPTZ     NOT NULL,
    fetched_at     TIMESTAMPTZ     NOT NULL,
    CONSTRAINT uq_exchange_rates_pair_quoted_at UNIQUE (base_currency, quote_currency, quoted_at),
    CONSTRAINT ck_exchange_rates_base_format CHECK (base_currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_exchange_rates_quote_format CHECK (quote_currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_exchange_rates_distinct_currencies CHECK (base_currency <> quote_currency),
    CONSTRAINT ck_exchange_rates_rate_positive CHECK (rate > 0)
);