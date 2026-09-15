-- V19 created several columns as SMALLINT (app_user.birth_month/birth_day,
-- holiday_definition.month/day/weekday/nth_week), but their entities map
-- them as plain Java Integer with no columnDefinition override --
-- Hibernate's default mapping for Integer is SQL INTEGER, not SMALLINT,
-- so schema validation (ddl-auto=validate) fails at startup with e.g.
-- "wrong column type encountered in column [birth_day] ... found
-- [int2 (Types#SMALLINT)], but expecting [integer (Types#INTEGER)]".
-- V19 already ran against real databases, so it can't be edited in place
-- (Flyway would reject the checksum mismatch) -- this widens the columns
-- instead. The existing CHECK constraints on app_user are range checks on
-- the value, not the storage width, so they still hold after the widen.
-- (Idempotent either way: ALTER ... TYPE INTEGER is a no-op if a column
-- is already INTEGER, so this is safe to re-run.)
ALTER TABLE app_user ALTER COLUMN birth_month TYPE INTEGER;
ALTER TABLE app_user ALTER COLUMN birth_day TYPE INTEGER;

ALTER TABLE holiday_definition ALTER COLUMN month TYPE INTEGER;
ALTER TABLE holiday_definition ALTER COLUMN day TYPE INTEGER;
ALTER TABLE holiday_definition ALTER COLUMN weekday TYPE INTEGER;
ALTER TABLE holiday_definition ALTER COLUMN nth_week TYPE INTEGER;
