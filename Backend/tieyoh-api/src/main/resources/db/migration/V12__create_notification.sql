CREATE TABLE notification (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_user(id),
    type VARCHAR(50) NOT NULL,
    actor_user_id BIGINT REFERENCES app_user(id),
    actor_display_name VARCHAR(255),
    message VARCHAR(500) NOT NULL,
    connection_request_id BIGINT REFERENCES connection_request(id),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_notification_user ON notification (user_id, created_at DESC);
