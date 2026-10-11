-- pricing-service gets permission-based gating on fares, fare rules and baggage policies, same shape
-- as AncillaryPermissions/SeatPermissions. Airline-scoped, so granted to airline roles only.
--
-- One new AIRLINE-scope role, resolved by name like every other role: REVENUE_MANAGER is the
-- commercial desk that files and maintains fares, distinct from the rules/liability owner (OWNER).
--
-- FARE_RULES_MANAGE is kept separate from FARE_MANAGE because refund and change terms are financial
-- liability (same reasoning as ANCILLARY_INSURANCE_MANAGE), so it is granted to OWNER only.

INSERT INTO roles (id, name, description, status, scope, created_at, updated_at) VALUES
    (UUID(), 'REVENUE_MANAGER', 'Files and maintains an airline''s fares, fare-to-cabin mapping and baggage policies (commercial desk)', 'ACTIVE', 'AIRLINE', NOW(), NOW());

INSERT INTO permissions (id, name, created_at, updated_at) VALUES
    (UUID(), 'FARE_READ', NOW(), NOW()),
    (UUID(), 'FARE_MANAGE', NOW(), NOW()),
    (UUID(), 'FARE_RULES_MANAGE', NOW(), NOW());

INSERT INTO role_permissions (id, role_id, permission_id, created_at)
SELECT UUID(), r.id, p.id, NOW() FROM roles r JOIN permissions p ON (r.name, p.name) IN (
    -- OWNER: holds everything, including rule/liability management.
    ('OWNER', 'FARE_READ'), ('OWNER', 'FARE_MANAGE'), ('OWNER', 'FARE_RULES_MANAGE'),

    -- ADMIN: day-to-day fare maintenance, but not refund/change rule terms.
    ('ADMIN', 'FARE_READ'), ('ADMIN', 'FARE_MANAGE'),

    -- REVENUE_MANAGER: fare filing and baggage policy, no rule terms.
    ('REVENUE_MANAGER', 'FARE_READ'), ('REVENUE_MANAGER', 'FARE_MANAGE')
);
