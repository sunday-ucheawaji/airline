-- booking-service gets permission-based gating on bookings, passengers and tickets, same shape as the other
-- airline-scoped domains. Airline-scoped permissions go only to airline roles; platform users (GDS_PLATFORM_ADMIN,
-- GDS_AUDITOR, SUPER_ADMIN) get BOOKING_SUPPORT_READ only, which returns passenger data masked.
--
-- Airline roles, resolved by name like every other role (no fixed id from config):
--   RESERVATIONS_OFFICER  books, changes and cancels bookings; reads passengers (no personal identifiers)
--   TICKETING_OFFICER     issues and voids tickets; reads passport and date of birth
--   REFUND_APPROVER       approves refunds; must not be the officer who requested the refund (enforced in the service)
--
-- TRAVELLER is the customer baseline role, granted to every non-staff user like AIRLINE_APPLICANT. It holds
-- the _OWN permissions, which the service also checks by comparing the caller with the booking's owner.

INSERT INTO roles (id, name, description, status, scope, created_at, updated_at) VALUES
    (UUID(), 'RESERVATIONS_OFFICER', 'Creates, changes and cancels an airline''s bookings', 'ACTIVE', 'AIRLINE', NOW(), NOW()),
    (UUID(), 'TICKETING_OFFICER', 'Issues and voids an airline''s tickets; sees passport and date of birth', 'ACTIVE', 'AIRLINE', NOW(), NOW()),
    (UUID(), 'REFUND_APPROVER', 'Approves refunds for an airline''s tickets (never their own requests)', 'ACTIVE', 'AIRLINE', NOW(), NOW()),
    (UUID(), 'TRAVELLER', 'Customer: books and manages their own trips', 'ACTIVE', 'BASELINE', NOW(), NOW());

INSERT INTO permissions (id, name, created_at, updated_at) VALUES
    (UUID(), 'BOOKING_READ', NOW(), NOW()),
    (UUID(), 'BOOKING_MANAGE', NOW(), NOW()),
    (UUID(), 'PASSENGER_READ', NOW(), NOW()),
    (UUID(), 'PASSENGER_PII_READ', NOW(), NOW()),
    (UUID(), 'TICKET_READ', NOW(), NOW()),
    (UUID(), 'TICKET_ISSUE', NOW(), NOW()),
    (UUID(), 'TICKET_VOID', NOW(), NOW()),
    (UUID(), 'TICKET_REFUND_APPROVE', NOW(), NOW()),
    (UUID(), 'BOOKING_SUPPORT_READ', NOW(), NOW()),
    (UUID(), 'BOOKING_CREATE_OWN', NOW(), NOW()),
    (UUID(), 'BOOKING_READ_OWN', NOW(), NOW()),
    (UUID(), 'BOOKING_CANCEL_OWN', NOW(), NOW()),
    (UUID(), 'PASSENGER_READ_OWN', NOW(), NOW()),
    (UUID(), 'TICKET_READ_OWN', NOW(), NOW());

INSERT INTO role_permissions (id, role_id, permission_id, created_at)
SELECT UUID(), r.id, p.id, NOW() FROM roles r JOIN permissions p ON (r.name, p.name) IN (
    -- SUPER_ADMIN and platform support: platform permission only, never an airline one
    ('SUPER_ADMIN', 'BOOKING_SUPPORT_READ'),
    ('GDS_PLATFORM_ADMIN', 'BOOKING_SUPPORT_READ'),
    ('GDS_AUDITOR', 'BOOKING_SUPPORT_READ'),

    -- OWNER and ADMIN: every airline booking function (fallback superset, as elsewhere)
    ('OWNER', 'BOOKING_READ'), ('OWNER', 'BOOKING_MANAGE'), ('OWNER', 'PASSENGER_READ'),
    ('OWNER', 'PASSENGER_PII_READ'), ('OWNER', 'TICKET_READ'), ('OWNER', 'TICKET_ISSUE'),
    ('OWNER', 'TICKET_VOID'), ('OWNER', 'TICKET_REFUND_APPROVE'),
    ('ADMIN', 'BOOKING_READ'), ('ADMIN', 'BOOKING_MANAGE'), ('ADMIN', 'PASSENGER_READ'),
    ('ADMIN', 'PASSENGER_PII_READ'), ('ADMIN', 'TICKET_READ'), ('ADMIN', 'TICKET_ISSUE'),
    ('ADMIN', 'TICKET_VOID'), ('ADMIN', 'TICKET_REFUND_APPROVE'),

    -- RESERVATIONS_OFFICER
    ('RESERVATIONS_OFFICER', 'BOOKING_READ'), ('RESERVATIONS_OFFICER', 'BOOKING_MANAGE'),
    ('RESERVATIONS_OFFICER', 'PASSENGER_READ'),

    -- TICKETING_OFFICER
    ('TICKETING_OFFICER', 'BOOKING_READ'), ('TICKETING_OFFICER', 'TICKET_READ'),
    ('TICKETING_OFFICER', 'TICKET_ISSUE'), ('TICKETING_OFFICER', 'TICKET_VOID'),
    ('TICKETING_OFFICER', 'PASSENGER_PII_READ'),

    -- REFUND_APPROVER
    ('REFUND_APPROVER', 'TICKET_READ'), ('REFUND_APPROVER', 'TICKET_REFUND_APPROVE'),

    -- TRAVELLER: customer self-service on their own bookings only
    ('TRAVELLER', 'BOOKING_CREATE_OWN'), ('TRAVELLER', 'BOOKING_READ_OWN'),
    ('TRAVELLER', 'BOOKING_CANCEL_OWN'), ('TRAVELLER', 'PASSENGER_READ_OWN'),
    ('TRAVELLER', 'TICKET_READ_OWN')
);
