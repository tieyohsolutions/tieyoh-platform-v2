-- Lets a notification point at the gift it's about (mirrors the existing
-- connection_request_id column) so tapping a GIFT_RECEIVED notification
-- in the app can open that gift's full-screen confirmation directly,
-- and a GIFT_COMMENT_RECEIVED notification can eventually do the same.
ALTER TABLE notification ADD COLUMN gift_id BIGINT;
