CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(200) NOT NULL UNIQUE,
    password_hash VARCHAR(200) NOT NULL,
    company VARCHAR(200),
    title VARCHAR(150),
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_APPROVAL',
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    approved_at TIMESTAMP
);

CREATE TABLE documents (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    category VARCHAR(100),
    file_path VARCHAR(500),
    external_url VARCHAR(500),
    uploaded_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE events (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    event_date TIMESTAMP NOT NULL,
    location VARCHAR(300),
    visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE publications (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    summary TEXT,
    content TEXT,
    external_url VARCHAR(500),
    visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',
    published_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by BIGINT REFERENCES users(id)
);

CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_events_date ON events(event_date);
CREATE INDEX idx_publications_published_at ON publications(published_at);
