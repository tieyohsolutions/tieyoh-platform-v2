-- New per-user privacy setting, alongside auto_accept_circle_invites
-- (V18): whether this user's gift sends default to visible to their
-- circle ("CONTACTS") or private ("PRIVATE") in the existing per-gift
-- visibility choice on the Send Gift screen. Defaults true (CONTACTS),
-- matching what GiftService already defaults new gifts to when no
-- visibility is specified -- this only changes the pre-selected default
-- shown to the sender, not the enforcement, which still happens per-gift.
ALTER TABLE app_user ADD COLUMN share_gifting_with_circle BOOLEAN NOT NULL DEFAULT TRUE;
