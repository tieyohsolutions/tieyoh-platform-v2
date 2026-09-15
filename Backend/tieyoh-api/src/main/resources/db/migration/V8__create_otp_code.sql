CREATE TABLE otp_code (
    id BIGSERIAL PRIMARY KEY,
    contact VARCHAR(255) NOT NULL,
    purpose VARCHAR(30) NOT NULL,
    code_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    consumed_at TIMESTAMP,
    attempt_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_otp_code_contact_purpose ON otp_code (contact, purpose);
