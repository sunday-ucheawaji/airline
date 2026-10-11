-- Aircraft was pre-existing and untouched by V8's access-model seed. It now gets real
-- permission-based gating: AIRCRAFT_MANAGE (write) and AIRCRAFT_READ (read), same shape as
-- ANCILLARY_MANAGE/ANCILLARY_INSURANCE_MANAGE. Airline-scoped, so granted to airline roles only.

INSERT INTO permissions (id, name, created_at, updated_at) VALUES
    (UUID(), 'AIRCRAFT_MANAGE', NOW(), NOW()),
    (UUID(), 'AIRCRAFT_READ', NOW(), NOW());

INSERT INTO role_permissions (id, role_id, permission_id, created_at)
SELECT UUID(), r.id, p.id, NOW() FROM roles r JOIN permissions p ON (r.name, p.name) IN (
    ('OWNER', 'AIRCRAFT_MANAGE'),
    ('OWNER', 'AIRCRAFT_READ'),
    ('ADMIN', 'AIRCRAFT_MANAGE'),
    ('ADMIN', 'AIRCRAFT_READ')
);
