-- The onboarding redesign now collects first and last name as two
-- separate fields (on the "Let's get started" screen) instead of a single
-- free-text display name. display_name stays as the existing NOT NULL
-- column everything else (search, feed, friend list, notifications, etc.)
-- already reads -- these are additive columns, not a replacement, so none
-- of that existing code needs to change.
ALTER TABLE app_user
    ADD COLUMN first_name VARCHAR(100),
    ADD COLUMN last_name VARCHAR(100);

-- Best-effort backfill for existing accounts: split the current
-- display_name on its first space. Anything with no space (a single
-- word, an empty edge case) just becomes the first name with no last
-- name, rather than guessing at a split.
UPDATE app_user
SET first_name = split_part(display_name, ' ', 1),
    last_name = NULLIF(trim(substring(display_name FROM position(' ' IN display_name) + 1)), '')
WHERE position(' ' IN display_name) > 0;

UPDATE app_user
SET first_name = display_name
WHERE first_name IS NULL;
