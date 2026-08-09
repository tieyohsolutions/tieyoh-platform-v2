CREATE TABLE parent_merchant (
    id BIGSERIAL PRIMARY KEY,
    pmid VARCHAR(50) NOT NULL UNIQUE,
    brand_name VARCHAR(255) NOT NULL,
    website VARCHAR(500),
    logo_url VARCHAR(1000),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE merchant
ADD COLUMN parent_merchant_id BIGINT;

ALTER TABLE merchant
ADD CONSTRAINT fk_merchant_parent
FOREIGN KEY (parent_merchant_id)
REFERENCES parent_merchant(id);

CREATE INDEX idx_merchant_parent_merchant_id
ON merchant(parent_merchant_id);