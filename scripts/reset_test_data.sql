-- Wipes all test users, invites, and circle links, keeping only the
-- account with Tieyoh ID @t-hg. Written 2026-09-02 to get a clean slate
-- for testing the invite/circle-linking fixes.
--
-- Run against your local dev Postgres, e.g.:
--   psql "postgresql://tieyoh:$DB_PASSWORD@localhost:5432/tieyoh" -f scripts/reset_test_data.sql
--
-- Safe to re-run. Aborts with an error (nothing is deleted) if no account
-- with tieyoh_id = '@t-hg' exists, so a typo here can't wipe every user.

BEGIN;

DO $$
DECLARE
    keep_id BIGINT;
BEGIN
    SELECT id INTO keep_id FROM app_user WHERE LOWER(tieyoh_id) = LOWER('@t-hg');
    IF keep_id IS NULL THEN
        RAISE EXCEPTION 'No account with tieyoh_id = @t-hg found -- aborting, nothing deleted.';
    END IF;
END $$;

-- Notifications and connection requests are transient/derived state tied
-- to the multi-user test data being wiped -- clear them outright rather
-- than trying to cherry-pick which ones still make sense.
DELETE FROM notification;
DELETE FROM connection_request;

-- Test gift-send records -- always involve at least one non-kept account
-- today, so there's nothing worth preserving here either.
DELETE FROM gift;

-- The actual "invites" and "links" the user asked to clean.
DELETE FROM invitation;
DELETE FROM friend_connection;

-- Stale email/phone verification codes from test signups.
DELETE FROM otp_code;

-- Drop other users' favorite-merchant lists (required before we can
-- delete those app_user rows); leaves @t-hg's own list untouched.
DELETE FROM user_merchant_list
WHERE user_id <> (SELECT id FROM app_user WHERE LOWER(tieyoh_id) = LOWER('@t-hg'));

-- Every account except @t-hg, including any incomplete signups that
-- never got a tieyoh_id at all.
DELETE FROM app_user
WHERE tieyoh_id IS NULL
   OR LOWER(tieyoh_id) <> LOWER('@t-hg');

COMMIT;

-- Verify: should show exactly one row.
SELECT id, tieyoh_id, email, display_name, status FROM app_user;
