CREATE TABLE merchant (
    id BIGSERIAL PRIMARY KEY,
    tmid VARCHAR(50) NOT NULL UNIQUE,
    brand_name VARCHAR(255) NOT NULL,
    website VARCHAR(500),
    phone VARCHAR(50),
    category VARCHAR(255),
    source VARCHAR(100),
    source_merchant_id VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE merchant_location (
    id BIGSERIAL PRIMARY KEY,
    merchant_id BIGINT NOT NULL REFERENCES merchant(id),
    location_name VARCHAR(255),
    address_line_1 VARCHAR(255),
    address_line_2 VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100) DEFAULT 'US',
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),
    phone VARCHAR(50),
    source_location_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_merchant_brand_name
    ON merchant(brand_name);

CREATE INDEX idx_merchant_location_postal_code
    ON merchant_location(postal_code);

CREATE INDEX idx_merchant_location_merchant_id
    ON merchant_location(merchant_id);
