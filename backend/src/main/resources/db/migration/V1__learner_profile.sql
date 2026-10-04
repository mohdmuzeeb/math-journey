-- AD-11: portable SQL (H2 and PostgreSQL). One learner per installation: exactly one row.
CREATE TABLE learner_profile (
    id               UUID         PRIMARY KEY,
    onboarding_stage VARCHAR(20)  NOT NULL
        CHECK (onboarding_stage IN ('welcome', 'prereq-check', 'journey')),
    sound            BOOLEAN      NOT NULL,
    companion_name   VARCHAR(40)  NOT NULL
);

-- Interim default until onboarding exists (epic 3 changes it for new installs with its own migration)
INSERT INTO learner_profile (id, onboarding_stage, sound, companion_name)
VALUES ('00000000-0000-0000-0000-000000000001', 'journey', TRUE, 'Lumi');
