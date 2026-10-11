CREATE TABLE IF NOT EXISTS permissions (
    id          CHAR(36) PRIMARY KEY,
    name        VARCHAR(150)  NOT NULL,
    description VARCHAR(500),
    created_at  DATETIME      NOT NULL,
    updated_at  DATETIME      NOT NULL,
    CONSTRAINT uk_permissions_name UNIQUE (name)
);


