-- flight-ops-service gets real permission-based gating (Flight/FlightSchedule/FlightInstance),
-- plus two new AIRLINE-scope roles narrower than ADMIN so day-to-day flight operations don't
-- require account-level staff: FLIGHT_DISPATCHER (routine flight/schedule/instance management)
-- and FLEET_ASSIGNMENT_OFFICER (only the aircraft-reassignment action). Explicit ids (14/15),
-- same precedent as OWNER/ADMIN/VIEWER = 3/4/5 in V8 — airline-core-service references them by
-- fixed id from config, same as airline.owner-role-id.

INSERT INTO roles (id, name, description, status, scope, created_at, updated_at) VALUES
    (14, 'FLIGHT_DISPATCHER', 'Manages an airline''s flights, schedules and flight instances day to day', 'ACTIVE', 'AIRLINE', NOW(), NOW()),
    (15, 'FLEET_ASSIGNMENT_OFFICER', 'Assigns or reassigns which aircraft operates a given flight instance', 'ACTIVE', 'AIRLINE', NOW(), NOW());

INSERT INTO permissions (name, created_at, updated_at) VALUES
    ('FLIGHT_MANAGE', NOW(), NOW()),
    ('FLIGHT_READ', NOW(), NOW()),
    ('FLIGHT_SCHEDULE_MANAGE', NOW(), NOW()),
    ('FLIGHT_SCHEDULE_READ', NOW(), NOW()),
    ('FLIGHT_INSTANCE_MANAGE', NOW(), NOW()),
    ('FLIGHT_INSTANCE_READ', NOW(), NOW()),
    ('FLIGHT_INSTANCE_AIRCRAFT_ASSIGN', NOW(), NOW());

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() FROM roles r JOIN permissions p ON (r.name, p.name) IN (
    ('SUPER_ADMIN', 'FLIGHT_MANAGE'), ('SUPER_ADMIN', 'FLIGHT_READ'),
    ('SUPER_ADMIN', 'FLIGHT_SCHEDULE_MANAGE'), ('SUPER_ADMIN', 'FLIGHT_SCHEDULE_READ'),
    ('SUPER_ADMIN', 'FLIGHT_INSTANCE_MANAGE'), ('SUPER_ADMIN', 'FLIGHT_INSTANCE_READ'),
    ('SUPER_ADMIN', 'FLIGHT_INSTANCE_AIRCRAFT_ASSIGN'),

    -- OWNER/ADMIN: fallback superset, same as every other airline-scoped domain
    ('OWNER', 'FLIGHT_MANAGE'), ('OWNER', 'FLIGHT_READ'),
    ('OWNER', 'FLIGHT_SCHEDULE_MANAGE'), ('OWNER', 'FLIGHT_SCHEDULE_READ'),
    ('OWNER', 'FLIGHT_INSTANCE_MANAGE'), ('OWNER', 'FLIGHT_INSTANCE_READ'),
    ('OWNER', 'FLIGHT_INSTANCE_AIRCRAFT_ASSIGN'),
    ('ADMIN', 'FLIGHT_MANAGE'), ('ADMIN', 'FLIGHT_READ'),
    ('ADMIN', 'FLIGHT_SCHEDULE_MANAGE'), ('ADMIN', 'FLIGHT_SCHEDULE_READ'),
    ('ADMIN', 'FLIGHT_INSTANCE_MANAGE'), ('ADMIN', 'FLIGHT_INSTANCE_READ'),
    ('ADMIN', 'FLIGHT_INSTANCE_AIRCRAFT_ASSIGN'),
    ('VIEWER', 'FLIGHT_READ'), ('VIEWER', 'FLIGHT_SCHEDULE_READ'), ('VIEWER', 'FLIGHT_INSTANCE_READ'),

    -- FLIGHT_DISPATCHER: routine flight-ops management, not aircraft reassignment.
    -- Needs AIRCRAFT_READ (already seeded by V9) to browse the fleet when picking an
    -- aircraftId at flight-instance/schedule creation time.
    ('FLIGHT_DISPATCHER', 'FLIGHT_MANAGE'), ('FLIGHT_DISPATCHER', 'FLIGHT_READ'),
    ('FLIGHT_DISPATCHER', 'FLIGHT_SCHEDULE_MANAGE'), ('FLIGHT_DISPATCHER', 'FLIGHT_SCHEDULE_READ'),
    ('FLIGHT_DISPATCHER', 'FLIGHT_INSTANCE_MANAGE'), ('FLIGHT_DISPATCHER', 'FLIGHT_INSTANCE_READ'),
    ('FLIGHT_DISPATCHER', 'AIRCRAFT_READ'),

    -- FLEET_ASSIGNMENT_OFFICER: only the aircraft-(re)assignment action, plus read visibility
    -- needed to do that job — no general flight/schedule/instance MANAGE, no AIRCRAFT_MANAGE
    -- (fleet CRUD stays OWNER/ADMIN's job).
    ('FLEET_ASSIGNMENT_OFFICER', 'FLIGHT_READ'), ('FLEET_ASSIGNMENT_OFFICER', 'FLIGHT_SCHEDULE_READ'),
    ('FLEET_ASSIGNMENT_OFFICER', 'FLIGHT_INSTANCE_READ'), ('FLEET_ASSIGNMENT_OFFICER', 'FLIGHT_INSTANCE_AIRCRAFT_ASSIGN'),
    ('FLEET_ASSIGNMENT_OFFICER', 'AIRCRAFT_READ')
);
