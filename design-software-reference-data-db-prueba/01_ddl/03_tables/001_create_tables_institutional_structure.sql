CREATE TABLE institutional_structure.macroregion (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    code        VARCHAR(10)  NOT NULL,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,

    CONSTRAINT uq_macroregion_code UNIQUE (code)
);

CREATE TABLE institutional_structure.microregion (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    macroregion_id  UUID         NOT NULL,
    name            VARCHAR(100) NOT NULL,
    code            VARCHAR(10)  NOT NULL,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,

    CONSTRAINT uq_microregion_macroregion_id_code
        UNIQUE (macroregion_id, code)
);

CREATE TABLE institutional_structure.department (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    microregion_id  UUID         NOT NULL,
    name            VARCHAR(100) NOT NULL,
    dane_code       VARCHAR(5)   NOT NULL,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,

    CONSTRAINT uq_department_dane_code UNIQUE (dane_code),
    CONSTRAINT uq_department_microregion_id_name
        UNIQUE (microregion_id, name)
);

CREATE TABLE institutional_structure.municipality (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    department_id   UUID         NOT NULL,
    name            VARCHAR(150) NOT NULL,
    dane_code       VARCHAR(8)   NOT NULL,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,

    CONSTRAINT uq_municipality_dane_code UNIQUE (dane_code),
    CONSTRAINT uq_municipality_department_id_name
        UNIQUE (department_id, name)
);

CREATE TABLE institutional_structure.training_center (
    id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    municipality_id  UUID          NOT NULL,
    center_code      VARCHAR(10)   NOT NULL,
    name             VARCHAR(200)  NOT NULL,
    address          TEXT,
    phone            VARCHAR(20),
    is_active        BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT uq_training_center_center_code
        UNIQUE (center_code)
);


CREATE TABLE institutional_structure.institutional_unit (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    training_center_id  UUID NOT NULL,
    name                VARCHAR(200) NOT NULL,
    unit_type           institutional_structure.institutional_unit_type NOT NULL,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);



