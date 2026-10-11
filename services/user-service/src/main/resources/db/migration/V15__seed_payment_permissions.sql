-- payment-service gets permission-based gating on payments, same shape as booking-service's V14. No new roles:
-- every grantee here already exists (OWNER/ADMIN, the airline officer roles from V14, TRAVELLER, platform staff).
--
-- PAYMENT_REFUND_EXECUTE is deliberately separate from TICKET_REFUND_APPROVE (V14): that permission is a
-- booking-domain decision ("does this ticket qualify for a refund"); this one is payment-service's own gate on
-- an irreversible money-movement action, kept independently revocable even though both are granted to the same
-- roles today. PAYMENT_SUPPORT_READ mirrors BOOKING_SUPPORT_READ: platform staff only, never an airline role.

INSERT INTO permissions (id, name, created_at, updated_at) VALUES
    (UUID(), 'PAYMENT_READ', NOW(), NOW()),
    (UUID(), 'PAYMENT_REFUND_EXECUTE', NOW(), NOW()),
    (UUID(), 'PAYMENT_READ_OWN', NOW(), NOW()),
    (UUID(), 'PAYMENT_SUPPORT_READ', NOW(), NOW());

INSERT INTO role_permissions (id, role_id, permission_id, created_at)
SELECT UUID(), r.id, p.id, NOW() FROM roles r JOIN permissions p ON (r.name, p.name) IN (
    -- SUPER_ADMIN and platform support: platform permission only, never an airline one
    ('SUPER_ADMIN', 'PAYMENT_SUPPORT_READ'),
    ('GDS_PLATFORM_ADMIN', 'PAYMENT_SUPPORT_READ'),
    ('GDS_AUDITOR', 'PAYMENT_SUPPORT_READ'),

    -- OWNER and ADMIN: every airline payment function (fallback superset, as elsewhere)
    ('OWNER', 'PAYMENT_READ'), ('OWNER', 'PAYMENT_REFUND_EXECUTE'),
    ('ADMIN', 'PAYMENT_READ'), ('ADMIN', 'PAYMENT_REFUND_EXECUTE'),

    -- RESERVATIONS_OFFICER / TICKETING_OFFICER: read only, no refund authority
    ('RESERVATIONS_OFFICER', 'PAYMENT_READ'),
    ('TICKETING_OFFICER', 'PAYMENT_READ'),

    -- REFUND_APPROVER: the same role that approves a ticket refund also executes the payment refund
    ('REFUND_APPROVER', 'PAYMENT_READ'), ('REFUND_APPROVER', 'PAYMENT_REFUND_EXECUTE'),

    -- TRAVELLER: customer self-service, own receipts only
    ('TRAVELLER', 'PAYMENT_READ_OWN')
);
