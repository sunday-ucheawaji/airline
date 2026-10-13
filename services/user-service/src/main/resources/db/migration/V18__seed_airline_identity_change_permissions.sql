-- Legal name, registration number and IATA/ICAO codes are verified during onboarding, so after provisioning they
-- change only through a request (airline-scoped, OWNER) approved by platform staff (platform-scoped).

INSERT INTO permissions (id, name, created_at, updated_at) VALUES
    (UUID(), 'AIRLINE_IDENTITY_CHANGE_REQUEST', NOW(), NOW()),
    (UUID(), 'AIRLINE_IDENTITY_CHANGE_REVIEW', NOW(), NOW());

INSERT INTO role_permissions (id, role_id, permission_id, created_at)
SELECT UUID(), r.id, p.id, NOW() FROM roles r JOIN permissions p ON (r.name, p.name) IN (
    ('OWNER', 'AIRLINE_IDENTITY_CHANGE_REQUEST'),
    ('SUPER_ADMIN', 'AIRLINE_IDENTITY_CHANGE_REVIEW'),
    ('COMPLIANCE_OFFICER', 'AIRLINE_IDENTITY_CHANGE_REVIEW')
);
