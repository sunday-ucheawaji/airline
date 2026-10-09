-- notification-service gets its first permission-gated endpoints: a delivery-history read and a
-- forced resend, both platform-only (a notification has no airline owner of its own to check
-- membership against — it's a side effect of a booking/payment event addressed to the customer),
-- same reasoning as BOOKING_SUPPORT_READ (V14) / PAYMENT_SUPPORT_READ (V15). No new roles needed.

INSERT INTO permissions (name, created_at, updated_at) VALUES
    ('NOTIFICATION_SUPPORT_READ', NOW(), NOW()),
    ('NOTIFICATION_RESEND', NOW(), NOW());

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() FROM roles r JOIN permissions p ON (r.name, p.name) IN (
    ('SUPER_ADMIN', 'NOTIFICATION_SUPPORT_READ'), ('SUPER_ADMIN', 'NOTIFICATION_RESEND'),
    ('GDS_PLATFORM_ADMIN', 'NOTIFICATION_SUPPORT_READ'), ('GDS_PLATFORM_ADMIN', 'NOTIFICATION_RESEND'),
    ('GDS_AUDITOR', 'NOTIFICATION_SUPPORT_READ')
);
