-- The TRAVELLER permissions are platform-scoped (they travel in the JWT), and SUPER_ADMIN holds every platform
-- permission. V14 granted them to TRAVELLER only; the gateway now enforces BOOKING_CREATE_OWN on POST /api/bookings.

INSERT INTO role_permissions (id, role_id, permission_id, created_at)
SELECT UUID(), r.id, p.id, NOW() FROM roles r JOIN permissions p ON p.name IN (
    'BOOKING_CREATE_OWN', 'BOOKING_READ_OWN', 'BOOKING_CANCEL_OWN', 'PASSENGER_READ_OWN', 'TICKET_READ_OWN')
WHERE r.name = 'SUPER_ADMIN';
