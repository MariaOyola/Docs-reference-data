CREATE TABLE parameterization.catalog (
    id           UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    code         VARCHAR(50)   NOT NULL,
    name         VARCHAR(100)  NOT NULL,
    description  TEXT,
    is_active    BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT uq_catalog_code
        UNIQUE (code)
);

CREATE TABLE parameterization.catalog_detail (
    id             UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    catalog_id     UUID          NOT NULL,
    code           VARCHAR(50)   NOT NULL,
    label          VARCHAR(255)  NOT NULL,
    display_order  INTEGER,
    is_active      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT uq_catalog_detail_catalog_id_code
        UNIQUE (catalog_id, code)
);

CREATE TABLE parameterization.parameter (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key          VARCHAR(100) NOT NULL,
    value        TEXT NOT NULL,
    value_type   parameterization.parameter_value_type NOT NULL,
    description  TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_parameter_key
        UNIQUE (key)
);
