CREATE TABLE user_merchant_list (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_user(id),
    merchant_id BIGINT NOT NULL REFERENCES merchant(id),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (user_id, merchant_id)
);

CREATE INDEX idx_user_merchant_list_user ON user_merchant_list(user_id);
