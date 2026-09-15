-- Backend for the "Interactive Social/Gifting Feed" spec: (1) an actual
-- gift-send/record table (there was none at all before this -- Send Gift
-- was a dead end past merchant selection), (2) a birthday field on
-- app_user (also never existed) so UPCOMING_BIRTHDAY events have
-- something to read, and (3) a small holiday_definition table so
-- UPCOMING_HOLIDAY events are configurable data, not hard-coded in the
-- app, per the spec's explicit requirement.

-- Month/day only, deliberately -- the spec calls out not exposing birth
-- year/age, and the feed only ever needs month+day to compute "how many
-- days until this person's birthday." No year column at all means there's
-- nothing sensitive to accidentally leak later.
ALTER TABLE app_user ADD COLUMN birth_month SMALLINT;
ALTER TABLE app_user ADD COLUMN birth_day SMALLINT;
ALTER TABLE app_user ADD CONSTRAINT chk_app_user_birth_month CHECK (birth_month IS NULL OR birth_month BETWEEN 1 AND 12);
ALTER TABLE app_user ADD CONSTRAINT chk_app_user_birth_day CHECK (birth_day IS NULL OR birth_day BETWEEN 1 AND 31);

-- A real, minimal gift-send record. No payment processing behind this yet
-- (per the user's own scoping: "no real payment processing yet") -- a
-- gift is recorded COMPLETED the moment it's sent, same as tapping "Send"
-- in the prototype UX always implied. sender_id/recipient_id are plain
-- FKs to app_user, matching how every other new-ish table in this schema
-- (friend_connection, notification) still works in numeric ids rather
-- than tID, since Gift is keyed by real, always-ACTIVE account ids on
-- both sides (you can't gift someone who hasn't joined Tieyoh).
CREATE TABLE gift (
    id BIGSERIAL PRIMARY KEY,
    sender_id BIGINT NOT NULL REFERENCES app_user(id),
    recipient_id BIGINT NOT NULL REFERENCES app_user(id),
    merchant_id BIGINT REFERENCES merchant(id),
    amount NUMERIC(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    -- PRIVATE (sender/recipient only), CONTACTS (also visible to anyone
    -- in either party's network -- direct circle or second-degree, the
    -- same "network connection" definition already established for the
    -- feed's second-degree view), or PUBLIC (any Tieyoh user). Default
    -- CONTACTS: visible within the existing social graph, never wide open
    -- by default. See FeedService for where this is actually enforced.
    visibility VARCHAR(20) NOT NULL DEFAULT 'CONTACTS',
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_gift_sender_created ON gift (sender_id, created_at DESC);
CREATE INDEX idx_gift_recipient_created ON gift (recipient_id, created_at DESC);
CREATE INDEX idx_gift_created_at ON gift (created_at DESC);

-- Configurable holiday calendar, per the spec's explicit "should
-- eventually be configurable rather than hard-coded into the mobile
-- application" -- doing that now rather than later since it's cheap to
-- get right from the start. rule_type is FIXED_DATE (month + day, e.g.
-- Christmas = 12/25) or NTH_WEEKDAY (month + iso weekday 1-7 + which
-- occurrence 1-4, e.g. Thanksgiving = November, Thursday(4), 4th one) --
-- needed because several well-known US holidays (Thanksgiving being the
-- spec's own example) don't fall on a fixed date every year.
CREATE TABLE holiday_definition (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    rule_type VARCHAR(20) NOT NULL,
    month SMALLINT NOT NULL,
    day SMALLINT,
    weekday SMALLINT,
    nth_week SMALLINT,
    display_message VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

-- Seed: general market/commercial calendar occasions only -- deliberately
-- excludes anything that would require inferring a user's religion,
-- culture, or personal observance (the spec's own caution). These six are
-- broadly treated as general shopping/gifting occasions in the US market
-- regardless of an individual's personal beliefs. `enabled` makes it easy
-- to turn any of these off later without a code change if that judgment
-- call needs revisiting.
INSERT INTO holiday_definition (name, rule_type, month, day, weekday, nth_week, display_message) VALUES
    ('New Year''s Day', 'FIXED_DATE', 1, 1, NULL, NULL, 'New Year''s Day is coming up. Send someone a Tieyoh gift.'),
    ('Valentine''s Day', 'FIXED_DATE', 2, 14, NULL, NULL, 'Valentine''s Day is coming up. Send someone a Tieyoh gift.'),
    ('Independence Day', 'FIXED_DATE', 7, 4, NULL, NULL, 'The 4th of July is coming up. Send someone a Tieyoh gift.'),
    ('Halloween', 'FIXED_DATE', 10, 31, NULL, NULL, 'Halloween is coming up. Send someone a Tieyoh gift.'),
    ('Thanksgiving', 'NTH_WEEKDAY', 11, NULL, 4, 4, 'Thanksgiving is coming up. Send someone a Tieyoh gift.'),
    ('Christmas', 'FIXED_DATE', 12, 25, NULL, NULL, 'Christmas is coming up. Send someone a Tieyoh gift.');
