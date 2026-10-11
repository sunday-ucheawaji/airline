CREATE TABLE IF NOT EXISTS roles (
    id          CHAR(36) PRIMARY KEY,
    name        VARCHAR(100)  NOT NULL,
    description VARCHAR(500),
    status      VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    scope       VARCHAR(20)   NOT NULL DEFAULT 'AIRLINE',
    created_at  DATETIME      NOT NULL,
    updated_at  DATETIME      NOT NULL,
    CONSTRAINT uk_roles_name UNIQUE (name)
);
