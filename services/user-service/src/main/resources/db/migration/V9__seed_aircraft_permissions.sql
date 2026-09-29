-- Aircraft was pre-existing and untouched by V8's access-model seed. It now gets real
-- permission-based gating: AIRCRAFT_MANAGE (write) and AIRCRAFT_READ (read), same shape as
-- ANCILLARY_MANAGE/ANCILLARY_INSURANCE_MANAGE. SUPER_ADMIN must hold every permission.

INSERT INTO permissions (name, created_at, updated_at) VALUES
    ('AIRCRAFT_MANAGE', NOW(), NOW()),
    ('AIRCRAFT_READ', NOW(), NOW());

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() FROM roles r JOIN permissions p ON (r.name, p.name) IN (
    ('SUPER_ADMIN', 'AIRCRAFT_MANAGE'),
    ('SUPER_ADMIN', 'AIRCRAFT_READ'),
    ('OWNER', 'AIRCRAFT_MANAGE'),
    ('OWNER', 'AIRCRAFT_READ'),
    ('ADMIN', 'AIRCRAFT_MANAGE'),
    ('ADMIN', 'AIRCRAFT_READ'),
    ('VIEWER', 'AIRCRAFT_READ')
);
