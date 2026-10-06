-- seat-service gets real permission-based gating (CabinClass/SeatMap/Seat configuration,
-- FlightInstanceCabin/SeatInstance provisioning, SeatInstance status transitions), same shape as
-- AncillaryPermissions/FlightOpsPermissions. Airline-scoped, so granted to airline roles only.
--
-- Two new AIRLINE-scope roles, explicit ids (16/17), same precedent as OWNER/ADMIN/VIEWER = 3/4/5
-- and FLIGHT_DISPATCHER/FLEET_ASSIGNMENT_OFFICER = 14/15: real airline PSS systems (Amadeus Altéa,
-- Sabre) organize this as two distinct operational functions, not a blanket read-only role —
-- Inventory Control (operational seat-availability desk) and Fleet/Cabin Configuration (structural
-- seat-map/cabin-layout design) — so VIEWER deliberately gets none of these permissions.
--
-- SEAT_INSTANCE_RELEASE is kept separate from SEAT_INSTANCE_STATUS_MANAGE because reversing a
-- seat back to AVAILABLE (cancellation/unblock/post-flight reset) is a materially different,
-- more sensitive action than moving inventory forward — same least-privilege reasoning as
-- FLIGHT_INSTANCE_AIRCRAFT_ASSIGN being split out in V10.

INSERT INTO roles (id, name, description, status, scope, created_at, updated_at) VALUES
    (16, 'SEAT_CONFIG_OFFICER', 'Manages an airline''s cabin classes, seat maps and seat templates (structural configuration)', 'ACTIVE', 'AIRLINE', NOW(), NOW()),
    (17, 'INVENTORY_CONTROL_OFFICER', 'Controls seat/fare availability for an airline''s flight instances (operational inventory desk)', 'ACTIVE', 'AIRLINE', NOW(), NOW());

INSERT INTO permissions (name, created_at, updated_at) VALUES
    ('SEAT_CONFIG_MANAGE', NOW(), NOW()),
    ('SEAT_READ', NOW(), NOW()),
    ('SEAT_INSTANCE_CONFIG_MANAGE', NOW(), NOW()),
    ('SEAT_INSTANCE_STATUS_MANAGE', NOW(), NOW()),
    ('SEAT_INSTANCE_RELEASE', NOW(), NOW());

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() FROM roles r JOIN permissions p ON (r.name, p.name) IN (
    -- OWNER/ADMIN: fallback superset, same as every other airline-scoped domain
    ('OWNER', 'SEAT_CONFIG_MANAGE'), ('OWNER', 'SEAT_READ'),
    ('OWNER', 'SEAT_INSTANCE_CONFIG_MANAGE'), ('OWNER', 'SEAT_INSTANCE_STATUS_MANAGE'),
    ('OWNER', 'SEAT_INSTANCE_RELEASE'),
    ('ADMIN', 'SEAT_CONFIG_MANAGE'), ('ADMIN', 'SEAT_READ'),
    ('ADMIN', 'SEAT_INSTANCE_CONFIG_MANAGE'), ('ADMIN', 'SEAT_INSTANCE_STATUS_MANAGE'),
    ('ADMIN', 'SEAT_INSTANCE_RELEASE'),

    -- SEAT_CONFIG_OFFICER: structural cabin/seat-map configuration only.
    ('SEAT_CONFIG_OFFICER', 'SEAT_CONFIG_MANAGE'), ('SEAT_CONFIG_OFFICER', 'SEAT_READ'),

    -- INVENTORY_CONTROL_OFFICER: the operational seat-availability desk.
    ('INVENTORY_CONTROL_OFFICER', 'SEAT_INSTANCE_CONFIG_MANAGE'),
    ('INVENTORY_CONTROL_OFFICER', 'SEAT_INSTANCE_STATUS_MANAGE'),
    ('INVENTORY_CONTROL_OFFICER', 'SEAT_INSTANCE_RELEASE'),
    ('INVENTORY_CONTROL_OFFICER', 'SEAT_READ')

    -- VIEWER intentionally gets nothing here — see header comment.
);
