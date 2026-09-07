-- The user's chosen profile picture, stored as a base64-encoded JPEG
-- (mobile already resizes it to a small max dimension before sending it,
-- so this stays reasonably sized). Nullable: most users won't have set one.
ALTER TABLE app_user
    ADD COLUMN profile_picture_base64 TEXT;
