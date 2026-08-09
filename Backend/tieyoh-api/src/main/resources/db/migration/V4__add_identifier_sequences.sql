CREATE SEQUENCE IF NOT EXISTS pmid_sequence
    START WITH 1
    INCREMENT BY 1;

CREATE SEQUENCE IF NOT EXISTS tmid_sequence
    START WITH 1
    INCREMENT BY 1;

DO $$
DECLARE
    max_pmid BIGINT;
    max_tmid BIGINT;
BEGIN
    SELECT COALESCE(
        MAX(CAST(SUBSTRING(pmid FROM 2) AS BIGINT)),
        0
    )
    INTO max_pmid
    FROM parent_merchant
    WHERE pmid ~ '^p[0-9]+$';

    IF max_pmid > 0 THEN
        PERFORM setval('pmid_sequence', max_pmid, true);
    ELSE
        PERFORM setval('pmid_sequence', 1, false);
    END IF;

    SELECT COALESCE(
        MAX(CAST(SUBSTRING(tmid FROM 2) AS BIGINT)),
        0
    )
    INTO max_tmid
    FROM merchant
    WHERE tmid ~ '^t[0-9]+$';

    IF max_tmid > 0 THEN
        PERFORM setval('tmid_sequence', max_tmid, true);
    ELSE
        PERFORM setval('tmid_sequence', 1, false);
    END IF;
END $$;