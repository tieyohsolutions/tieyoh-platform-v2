-- Safety net alongside the app-level dedup in FriendService.addFriend: at
-- most one circle entry per (owner, phone) and (owner, email) among rows
-- that aren't yet linked to a real account. Rows already linked to a real
-- account are covered separately by idx_friend_connection_owner_contact
-- (V10). Existing duplicate rows (created before this fix) are collapsed
-- first, keeping the oldest row per key, so the index can be created.

DELETE FROM friend_connection fc
    USING friend_connection older
    WHERE fc.contact_user_id IS NULL
    AND older.contact_user_id IS NULL
    AND fc.phone IS NOT NULL
    AND fc.phone = older.phone
    AND fc.owner_user_id = older.owner_user_id
    AND fc.id > older.id;

DELETE FROM friend_connection fc
    USING friend_connection older
    WHERE fc.contact_user_id IS NULL
    AND older.contact_user_id IS NULL
    AND fc.email IS NOT NULL
    AND LOWER(fc.email) = LOWER(older.email)
    AND fc.owner_user_id = older.owner_user_id
    AND fc.id > older.id;

CREATE UNIQUE INDEX idx_friend_connection_owner_pending_phone
    ON friend_connection (owner_user_id, phone)
    WHERE contact_user_id IS NULL AND phone IS NOT NULL;

CREATE UNIQUE INDEX idx_friend_connection_owner_pending_email
    ON friend_connection (owner_user_id, LOWER(email))
    WHERE contact_user_id IS NULL AND email IS NOT NULL;
