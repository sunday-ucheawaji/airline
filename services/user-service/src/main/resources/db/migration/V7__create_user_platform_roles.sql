CREATE TABLE IF NOT EXISTS user_platform_roles (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT    NOT NULL,
    role_id     BIGINT    NOT NULL,
    created_at  DATETIME  NOT NULL,
    CONSTRAINT uk_user_platform_roles_user_role UNIQUE (user_id, role_id),
    CONSTRAINT fk_user_platform_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_platform_roles_role FOREIGN KEY (role_id) REFERENCES roles (id)
);
