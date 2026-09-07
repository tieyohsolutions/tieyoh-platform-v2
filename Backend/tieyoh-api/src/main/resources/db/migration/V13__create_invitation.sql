CREATE TABLE invitation (
    id BIGSERIAL PRIMARY KEY,
    inviter_user_id BIGINT NOT NULL REFERENCES app_user(id),
    recipient_name VARCHAR(255) NOT NULL,
    recipient_email_hash VARCHAR(64),
    recipient_phone_hash VARCHAR(64),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    expires_at TIMESTAMP NOT NULL,
    accepted_user_id BIGINT REFERENCES app_user(id),
    accepted_at TIMESTAMP
);

CREATE INDEX idx_invitation_inviter ON invitation (inviter_user_id);
CREATE INDEX idx_invitation_token_hash ON invitation (token_hash);
