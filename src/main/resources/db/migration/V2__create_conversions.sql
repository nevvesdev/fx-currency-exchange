CREATE TABLE conversions (
    id              UUID            PRIMARY KEY,
    source_currency VARCHAR(3)      NOT NULL,
    source_amount   NUMERIC(28, 4)  NOT NULL,
    result_currency VARCHAR(3)      NOT NULL,
    result_amount   NUMERIC(28, 4)  NOT NULL,
    rate            NUMERIC(40, 20) NOT NULL,
    rate_quoted_at  TIMESTAMPTZ     NOT NULL,
    converted_at    TIMESTAMPTZ     NOT NULL,
    CONSTRAINT ck_conversions_source_format CHECK (source_currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_conversions_result_format CHECK (result_currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_conversions_distinct_currencies CHECK (source_currency <> result_currency),
    CONSTRAINT ck_conversions_source_amount_positive CHECK (source_amount > 0),
    CONSTRAINT ck_conversions_result_amount_non_negative CHECK (result_amount >= 0),
    CONSTRAINT ck_conversions_rate_positive CHECK (rate > 0)
);

CREATE INDEX ix_conversions_converted_at ON conversions (converted_at DESC);