CREATE TABLE connection_request (
    id BIGSERIAL PRIMARY KEY,
    sender_id BIGINT NOT NULL REFERENCES app_user(id),
    recipient_id BIGINT NOT NULL REFERENCES app_user(id),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_connection_request_sender ON connection_request (sender_id);
CREATE INDEX idx_connection_request_recipient ON connection_request (recipient_id);

-- Only one open request per direction between the same pair of people.
CREATE UNIQUE INDEX idx_connection_request_pending_pair
    ON connection_request (sender_id, recipient_id)
    WHERE status = 'PENDING';
