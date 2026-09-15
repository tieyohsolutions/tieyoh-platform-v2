-- FriendService/InvitationService normalize an invited contact's phone
-- (digits + optional leading '+') before storing or matching it, but
-- app_user.phone was only ever trimmed at profile setup, never
-- normalized, until this same release. The two sides drifted for every
-- account created before this fix, which silently broke the "pending
-- invite promotes to ACTIVE once they sign up" matching for phone-based
-- invites (email was unaffected -- it was already lowercased on both
-- sides). This migration backfills existing data to the same shape and
-- retroactively runs the promotion that should have already happened.

-- Drop the pending-phone uniqueness constraint from V14 first: normalizing
-- can make two previously-distinct raw values collide for the same owner,
-- so duplicates need to be re-collapsed below before the index comes back.
DROP INDEX IF EXISTS idx_friend_connection_owner_pending_phone;

UPDATE app_user
SET phone = CASE
    WHEN phone LIKE '+%' THEN '+' || regexp_replace(substring(phone from 2), '[^0-9]', '', 'g')
    ELSE regexp_replace(phone, '[^0-9]', '', 'g')
END
WHERE phone IS NOT NULL;

UPDATE friend_connection
SET phone = CASE
    WHEN phone LIKE '+%' THEN '+' || regexp_replace(substring(phone from 2), '[^0-9]', '', 'g')
    ELSE regexp_replace(phone, '[^0-9]', '', 'g')
END
WHERE phone IS NOT NULL;

-- Re-collapse any rows that just became duplicates of each other now that
-- their phone values are normalized (same rule as V14: keep the oldest
-- row per owner+phone among rows not yet linked to a real account).
DELETE FROM friend_connection fc
    USING friend_connection older
    WHERE fc.contact_user_id IS NULL
    AND older.contact_user_id IS NULL
    AND fc.phone IS NOT NULL
    AND fc.phone = older.phone
    AND fc.owner_user_id = older.owner_user_id
    AND fc.id > older.id;

CREATE UNIQUE INDEX idx_friend_connection_owner_pending_phone
    ON friend_connection (owner_user_id, phone)
    WHERE contact_user_id IS NULL AND phone IS NOT NULL;

-- Retroactively promote pending circle entries that should already have
-- linked up -- inviting someone by phone before they signed up, where the
-- format mismatch previously kept the automatic promotion from firing.
UPDATE friend_connection fc
SET contact_user_id = u.id,
    display_name = u.display_name,
    tieyoh_id = u.tieyoh_id,
    status = 'ACTIVE',
    updated_at = now()
FROM app_user u
WHERE fc.status = 'PENDING'
AND fc.contact_user_id IS NULL
AND fc.phone IS NOT NULL
AND fc.phone = u.phone
AND fc.owner_user_id <> u.id;

-- Same idea for email, defensively -- IgnoreCase queries already tolerated
-- case differences, but not stray whitespace.
UPDATE friend_connection fc
SET contact_user_id = u.id,
    display_name = u.display_name,
    tieyoh_id = u.tieyoh_id,
    status = 'ACTIVE',
    updated_at = now()
FROM app_user u
WHERE fc.status = 'PENDING'
AND fc.contact_user_id IS NULL
AND fc.email IS NOT NULL
AND LOWER(TRIM(fc.email)) = LOWER(TRIM(u.email))
AND fc.owner_user_id <> u.id;
