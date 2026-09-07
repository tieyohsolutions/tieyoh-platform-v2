CREATE TABLE friend_connection (
    id BIGSERIAL PRIMARY KEY,
    owner_user_id BIGINT NOT NULL REFERENCES app_user(id),
    contact_user_id BIGINT REFERENCES app_user(id),
    display_name VARCHAR(255) NOT NULL,
    phone VARCHAR(30),
    email VARCHAR(255),
    tieyoh_id VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_friend_connection_owner ON friend_connection (owner_user_id);
CREATE INDEX idx_friend_connection_phone ON friend_connection (phone) WHERE phone IS NOT NULL;
CREATE INDEX idx_friend_connection_email ON friend_connection (LOWER(email)) WHERE email IS NOT NULL;

-- Keep at most one circle entry per (owner, real Tieyoh account) pair --
-- re-adding someone already in the circle should update that row, not
-- duplicate it.
CREATE UNIQUE INDEX idx_friend_connection_owner_contact
    ON friend_connection (owner_user_id, contact_user_id)
    WHERE contact_user_id IS NOT NULL;
