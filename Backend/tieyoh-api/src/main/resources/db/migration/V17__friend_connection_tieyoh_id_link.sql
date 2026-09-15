-- The @t- Tieyoh ID, not the internal numeric account id, is the real
-- identifier for a friend_connection's owner -- requested directly by the
-- user, who pointed out that owner_user_id/contact_user_id (opaque
-- internal ids) shouldn't be what defines this relationship. Every owner
-- of a circle is, by definition, a completed (ACTIVE) account with a
-- tieyoh_id by the time they reach any screen that creates these rows, so
-- owner_user_id is dropped entirely in favor of owner_tieyoh_id.
--
-- On the contact side, contact_user_id is kept as a numeric convenience
-- reference for the parts of the app that still work in numeric ids
-- (profile pictures, the feed's second-degree traversal, gift and
-- connection-request flows), but the existing "tieyoh_id" column -- which
-- already gets populated once a contact is a real, joined member -- is
-- renamed to contact_tieyoh_id here, both for symmetry with the new owner
-- column and to make clear it's the authoritative "is this really a
-- joined Tieyoh member" identifier, not contact_user_id.

ALTER TABLE friend_connection ADD COLUMN owner_tieyoh_id VARCHAR(50);

UPDATE friend_connection fc
SET owner_tieyoh_id = u.tieyoh_id
FROM app_user u
WHERE fc.owner_user_id = u.id;

-- Defensive, not expected to match anything: every existing row's owner
-- should already be a real ACTIVE user with a tieyoh_id, since only
-- completed accounts ever reach the screens that create these rows. If
-- one somehow doesn't, drop that orphaned row rather than leave a circle
-- entry with no valid owner identifier at all.
DELETE FROM friend_connection WHERE owner_tieyoh_id IS NULL;

ALTER TABLE friend_connection ALTER COLUMN owner_tieyoh_id SET NOT NULL;

DROP INDEX IF EXISTS idx_friend_connection_owner;
DROP INDEX IF EXISTS idx_friend_connection_owner_contact;
DROP INDEX IF EXISTS idx_friend_connection_owner_pending_phone;
DROP INDEX IF EXISTS idx_friend_connection_owner_pending_email;

ALTER TABLE friend_connection DROP COLUMN owner_user_id;

CREATE INDEX idx_friend_connection_owner_tieyoh_id ON friend_connection (owner_tieyoh_id);

-- Keep at most one circle entry per (owner, real Tieyoh account) pair.
CREATE UNIQUE INDEX idx_friend_connection_owner_contact
    ON friend_connection (owner_tieyoh_id, contact_user_id)
    WHERE contact_user_id IS NOT NULL;

-- At most one circle entry per (owner, phone) and (owner, email) among
-- rows not yet linked to a real account.
CREATE UNIQUE INDEX idx_friend_connection_owner_pending_phone
    ON friend_connection (owner_tieyoh_id, phone)
    WHERE contact_user_id IS NULL AND phone IS NOT NULL;

CREATE UNIQUE INDEX idx_friend_connection_owner_pending_email
    ON friend_connection (owner_tieyoh_id, LOWER(email))
    WHERE contact_user_id IS NULL AND email IS NOT NULL;

ALTER TABLE friend_connection RENAME COLUMN tieyoh_id TO contact_tieyoh_id;
