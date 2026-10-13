-- Editing and closing an airline used to need only an active membership of any role, so a VIEWER could
-- rewrite or close the airline. Airline-scoped, so granted to airline roles only (never SUPER_ADMIN or VIEWER).

INSERT INTO permissions (id, name, created_at, updated_at) VALUES
    (UUID(), 'AIRLINE_PROFILE_UPDATE', NOW(), NOW()),
    (UUID(), 'AIRLINE_CLOSE', NOW(), NOW());

INSERT INTO role_permissions (id, role_id, permission_id, created_at)
SELECT UUID(), r.id, p.id, NOW() FROM roles r JOIN permissions p ON (r.name, p.name) IN (
    ('OWNER', 'AIRLINE_PROFILE_UPDATE'),
    ('OWNER', 'AIRLINE_CLOSE'),
    ('ADMIN', 'AIRLINE_PROFILE_UPDATE')
);
