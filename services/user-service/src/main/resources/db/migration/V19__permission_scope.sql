-- Permissions get an explicit scope so the API can refuse grants across it: PLATFORM permissions travel in the JWT
-- and go to PLATFORM/BASELINE roles; AIRLINE permissions are checked per airline membership and go to AIRLINE roles.

ALTER TABLE permissions ADD COLUMN scope VARCHAR(20) NOT NULL DEFAULT 'PLATFORM';

-- V8 also granted these three platform permissions to OWNER/ADMIN. Airline roles never reach the JWT, so those
-- grants never had any effect; remove them so no permission is granted across scopes.
DELETE rp FROM role_permissions rp
    JOIN roles r ON r.id = rp.role_id
    JOIN permissions p ON p.id = rp.permission_id
WHERE r.scope = 'AIRLINE' AND p.name IN ('AIRLINE_READ', 'AIRLINE_UPDATE', 'USER_READ');

UPDATE permissions p SET p.scope = 'AIRLINE'
WHERE EXISTS (
    SELECT 1 FROM role_permissions rp JOIN roles r ON r.id = rp.role_id
    WHERE rp.permission_id = p.id AND r.scope = 'AIRLINE'
);
