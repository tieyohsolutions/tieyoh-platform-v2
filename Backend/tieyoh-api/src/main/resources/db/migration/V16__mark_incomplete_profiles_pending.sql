-- Backfill for accounts that finished signup (email/password) but never
-- completed the "choose your Tieyoh ID" profile setup screen -- these were
-- previously left at status='ACTIVE' with tieyoh_id still NULL, which is
-- how e.g. "Test2 Test2" could exist and be discoverable with no @t id.
-- New signups are now created as PENDING_PROFILE (see AuthService.signup)
-- and only flip to ACTIVE once profile setup completes (see
-- UserProfileService.saveProfile). This retroactively marks any existing
-- account matching that same incomplete shape.
UPDATE app_user
SET status = 'PENDING_PROFILE'
WHERE tieyoh_id IS NULL
  AND status = 'ACTIVE';

-- If anyone's circle had already resolved one of these incomplete accounts
-- to a full ACTIVE entry (addFriend previously didn't check status when
-- matching an invite's phone/email to an existing account), that entry
-- needs to go back to a plain pending invite -- from the inviter's point
-- of view this person hasn't actually joined Tieyoh yet. If the owner
-- already has a separate pending entry for the same phone/email, demoting
-- this row too would collide with the partial unique indexes from V14, so
-- in that rare case just drop the now-redundant row instead of merging it.
DELETE FROM friend_connection fc
USING app_user u
WHERE fc.contact_user_id = u.id
  AND u.status = 'PENDING_PROFILE'
  AND fc.status = 'ACTIVE'
  AND (
    (fc.phone IS NOT NULL AND EXISTS (
        SELECT 1 FROM friend_connection other
        WHERE other.owner_user_id = fc.owner_user_id
          AND other.contact_user_id IS NULL
          AND other.phone = fc.phone
    ))
    OR
    (fc.email IS NOT NULL AND EXISTS (
        SELECT 1 FROM friend_connection other
        WHERE other.owner_user_id = fc.owner_user_id
          AND other.contact_user_id IS NULL
          AND LOWER(other.email) = LOWER(fc.email)
    ))
  );

UPDATE friend_connection fc
SET status = 'PENDING',
    contact_user_id = NULL,
    tieyoh_id = NULL
FROM app_user u
WHERE fc.contact_user_id = u.id
  AND u.status = 'PENDING_PROFILE'
  AND fc.status = 'ACTIVE';
