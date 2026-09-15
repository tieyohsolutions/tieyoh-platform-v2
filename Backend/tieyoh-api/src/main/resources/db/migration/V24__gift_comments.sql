-- Backs the "thank the sender" flow: a short free-text or emoji reply on
-- a specific gift, restricted in application code to that gift's sender
-- and recipient (see GiftService.addComment). This is the minimal slice
-- of the spec's broader "sender or receiver comments" item -- just
-- enough for a one-shot thank-you, not a full comment thread UI yet.
CREATE TABLE gift_comment (
    id BIGSERIAL PRIMARY KEY,
    gift_id BIGINT NOT NULL REFERENCES gift(id),
    author_user_id BIGINT NOT NULL REFERENCES app_user(id),
    author_display_name VARCHAR(255) NOT NULL,
    body VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_gift_comment_gift_id ON gift_comment(gift_id);
