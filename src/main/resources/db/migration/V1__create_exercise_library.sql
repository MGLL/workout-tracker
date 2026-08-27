-- Public, shared exercise catalog. Language-neutral facts live on `exercise`;
-- anything a human reads lives in `exercise_translation`, one row per language.

CREATE TABLE exercise (
    id           UUID         PRIMARY KEY,
    code         VARCHAR(100) NOT NULL UNIQUE,
    muscle_group VARCHAR(32)  NOT NULL,
    body_part    VARCHAR(32)  NOT NULL,
    equipment    VARCHAR(32),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

COMMENT ON COLUMN exercise.code IS 'Stable language-neutral slug, e.g. bench-press';
COMMENT ON COLUMN exercise.body_part IS 'Drives the progressive overload increment: +2.5kg upper, +5kg lower';

-- No CHECK on locale: adding a language must be an INSERT, never a migration.
-- The set of accepted locales is validated by the application (app.i18n.supported-locales).
CREATE TABLE exercise_translation (
    exercise_id UUID         NOT NULL REFERENCES exercise (id) ON DELETE CASCADE,
    locale      VARCHAR(8)   NOT NULL,
    name        VARCHAR(150) NOT NULL,
    description TEXT,
    PRIMARY KEY (exercise_id, locale)
);

-- The catalog is shared by every user, so the same name must not be added twice.
CREATE UNIQUE INDEX uq_exercise_translation_locale_name
    ON exercise_translation (locale, lower(name));

CREATE INDEX idx_exercise_muscle_group ON exercise (muscle_group);
CREATE INDEX idx_exercise_body_part ON exercise (body_part);
