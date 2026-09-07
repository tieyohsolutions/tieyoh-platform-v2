ALTER TABLE app_user
    ADD COLUMN tieyoh_id VARCHAR(50),
    ADD COLUMN phone VARCHAR(30),
    ADD COLUMN address VARCHAR(255),
    ADD COLUMN gender VARCHAR(20);

CREATE UNIQUE INDEX idx_app_user_tieyoh_id ON app_user (LOWER(tieyoh_id)) WHERE tieyoh_id IS NOT NULL;
