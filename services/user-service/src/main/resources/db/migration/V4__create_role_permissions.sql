CREATE TABLE IF NOT EXISTS role_permissions (
    id            CHAR(36) PRIMARY KEY,
    role_id       CHAR(36) NOT NULL,
    permission_id CHAR(36) NOT NULL,
    created_at    DATETIME  NOT NULL,
    CONSTRAINT uk_role_permissions_role_permission UNIQUE (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles (id),
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions (id)
);
