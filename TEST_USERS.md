# Test users — Zephyr Airways E2E pass (2026-10-10)

All personas share the password `Passw0rd!123` except the seeded super admin. Every user's email is already verified. Login: `POST /auth/login` on the gateway (`http://localhost:5000`), body `{"email": "...", "password": "..."}`, response field `jwt`. Access tokens expire in 15 minutes — re-login as needed.

Airline: **Zephyr Airways** (`id: 01a12393-5615-702c-aed6-5223f7a28ace`, IATA `ZP`, ICAO `ZEP`, Nigeria).

## Platform staff

| Email | Password | User ID | Platform role |
|---|---|---|---|
| `sunday@gmail.com` | `codewithzosh` | `01a1237b-c541-701a-960f-155d8d2c7cac` | SUPER_ADMIN (seeded, local-profile only) |
| `officer1@example.com` | `Passw0rd!123` | `01a1238b-923b-7523-bc5a-398d28cec1b8` | ONBOARDING_OFFICER |
| `compliance1@example.com` | `Passw0rd!123` | `01a1238b-9449-7be9-a395-9e5b31c7e3af` | COMPLIANCE_OFFICER |
| `commercial1@example.com` | `Passw0rd!123` | `01a1238b-9789-734f-b990-009af6724996` | COMMERCIAL_OFFICER |
| `technical1@example.com` | `Passw0rd!123` | `01a1238b-9a52-76b4-9bbc-1d710148f74d` | TECHNICAL_OFFICER |
| `approver1@example.com` | `Passw0rd!123` | `01a1238b-a0b2-7e97-8f36-0008537e9dce` | SENIOR_APPROVER |
| `provisioner1@example.com` | `Passw0rd!123` | `01a1238b-a72f-74c7-ab7b-e9f61aaab899` | AIRLINE_PROVISIONER |
| `platformadmin1@example.com` | `Passw0rd!123` | `01a1238b-adff-7ff2-bb32-52d935e256bb` | GDS_PLATFORM_ADMIN |
| `auditor1@example.com` | `Passw0rd!123` | `01a1238b-b07d-72ca-8a60-ab4191b26a38` | GDS_AUDITOR |

## Applicant (baseline, no airline membership)

| Email | Password | User ID | Role |
|---|---|---|---|
| `applicant1@example.com` | `Passw0rd!123` | `01a1238b-8da3-7de9-8a71-eada98aa1379` | AIRLINE_APPLICANT / TRAVELLER (applied for and owns the onboarding application that provisioned Zephyr Airways; not automatically a member) |

## Zephyr Airways members

| Email | Password | User ID | Airline role |
|---|---|---|---|
| `owner1@example.com` | `Passw0rd!123` | `01a1238b-b8d1-7408-a7dd-409be5a3053e` | OWNER (nominated at provisioning) |
| `admin1@example.com` | `Passw0rd!123` | `01a1238b-bd25-7219-b7d1-5d739f0ba89c` | ADMIN |
| `viewer1@example.com` | `Passw0rd!123` | `01a1238b-c081-7526-9409-31c9b187b057` | VIEWER |
| `dispatcher1@example.com` | `Passw0rd!123` | `01a1238b-c366-78b0-b68d-385d76c4390b` | FLIGHT_DISPATCHER |
| `fleetofficer1@example.com` | `Passw0rd!123` | `01a1238b-c71a-7514-aea1-1cdb76ab5a6c` | FLEET_ASSIGNMENT_OFFICER |
| `seatconfig1@example.com` | `Passw0rd!123` | `01a1238b-c9e8-7fe5-844e-88cfc8ea8da7` | SEAT_CONFIG_OFFICER |
| `inventory1@example.com` | `Passw0rd!123` | `01a1238b-cbec-76c7-8fc4-e5a206a566d7` | INVENTORY_CONTROL_OFFICER |

## Customers (baseline, no airline membership)

| Email | Password | User ID | Notes |
|---|---|---|---|
| `customer1@example.com` | `Passw0rd!123` | `01a1238b-cea1-75ef-93c0-8167e7dcc956` | Has booking `BK89D66AE3` (PENDING, Razorpay payment link created, never completed — see gap report) |
| `customer2@example.com` | `Passw0rd!123` | `01a1238b-d057-78bc-a23e-811b6a5dc3e9` | Had booking `BK29B65D4D`, cancelled; seat released, BOOKING_CANCELLED notification sent (email) |

## Fixtures created this pass

- **Aircraft**: `5N-ZPY` (A320-200, 132 economy + 18 business, `id: 01a12394-f536-7567-b96a-33b75d1548a3`), `5N-ZPZ` (A321-200, 150 economy + 30 business, `id: 01a12398-e208-769a-98dd-61d3df3b8dc0`).
- **Flight**: `ZP101`, Lagos (LOS) → London Heathrow (LHR), `id: 01a12395-931b-70b1-a86b-a4b6acc1421c`.
- **Flight schedule**: daily, 2026-11-01 → 2026-11-10 (10 instances). First two instances reassigned to `5N-ZPZ`; the rest still on `5N-ZPY`.
- **Fare**: "Economy Flex", `id: 01a123a1-a25e-72f4-9b20-6417a6134a9d`, PUBLISHED, $305 total (with fare rules + baggage policy attached).
- **Ancillary offering**: "Extra Checked Bag" on this flight's economy cabin, $35, offering id `01a123a3-7ad9-7c1e-8688-379f599a96c5`.
- **Meal offering**: "Chicken & Rice" on this flight, $15, offering id `01a123a3-7fa8-76d7-a28b-85a329d8ecd7`.
