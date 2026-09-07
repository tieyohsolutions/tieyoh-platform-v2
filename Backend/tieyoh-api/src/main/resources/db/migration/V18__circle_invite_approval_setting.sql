-- New per-user setting, requested by the user: "auto-accept circle
-- invites" (default true, matching today's existing frictionless
-- behavior) vs. requiring the user's approval before someone who adds
-- them becomes mutually connected. Defaults true for every existing
-- account too, since none of them could have opted out before this
-- setting existed.
ALTER TABLE app_user ADD COLUMN auto_accept_circle_invites BOOLEAN NOT NULL DEFAULT TRUE;

-- Adding someone to your circle is now mutual: the person you add also
-- gets you in their own circle (immediately if they auto-accept, or once
-- they approve if they don't). Existing circle links were one-directional
-- only -- backfill the missing reverse side for every relationship that's
-- already ACTIVE, since everyone defaults to auto-accept as of this
-- migration and this is exactly what would have happened automatically
-- had this feature existed when those adds were made.
INSERT INTO friend_connection (
    owner_tieyoh_id, contact_user_id, display_name, phone, email,
    contact_tieyoh_id, status, created_at, updated_at
)
SELECT
    contact.tieyoh_id,
    owner_user.id,
    owner_user.display_name,
    owner_user.phone,
    owner_user.email,
    owner_user.tieyoh_id,
    'ACTIVE',
    now(),
    now()
FROM friend_connection fc
JOIN app_user owner_user ON owner_user.tieyoh_id = fc.owner_tieyoh_id
JOIN app_user contact ON contact.id = fc.contact_user_id
WHERE fc.status = 'ACTIVE'
  AND owner_user.id <> contact.id
  AND NOT EXISTS (
      SELECT 1 FROM friend_connection reverse_fc
      WHERE reverse_fc.owner_tieyoh_id = contact.tieyoh_id
        AND reverse_fc.contact_user_id = owner_user.id
  );
