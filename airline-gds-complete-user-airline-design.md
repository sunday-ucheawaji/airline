# Airline-Only GDS: User Service and Airline Service Implementation Plan

## 1. Document Purpose

This document consolidates the agreed design for the first phase of an
**airline-only Global Distribution System (GDS)**.

The scope covers:

-   User registration and authentication
-   Roles and permissions
-   Airline onboarding and document submission
-   Administrative review and approval
-   Airline creation
-   Airline memberships
-   Airline-specific roles
-   Employee invitations
-   Private document storage
-   Authorization within an airline context

This phase does **not** implement:

-   Flights
-   Aircraft
-   Schedules
-   Seat inventory
-   Shopping/search
-   Fare management
-   Booking/PNR
-   Ticketing
-   Payments
-   Refund processing
-   External airline integrations

Those capabilities can be built after the identity, organization,
onboarding, and authorization foundations are stable.

------------------------------------------------------------------------

# 1a. Implementation Status (updated 2026-09-18)

This document was written as a greenfield design. **`services/user-service` has
since been built against it incrementally**, inside a larger pre-existing
microservices repo (not greenfield) — see the repo-root `CLAUDE.md` for the
full architectural context. The sections below are now annotated against what
was actually shipped and manually end-to-end tested. **The Airline Service
(section 10 onward) has not been started at all** — those sections remain
purely aspirational.

Headline divergences from this doc, all deliberate:

-   **IDs are `Long` (MySQL `BIGINT AUTO_INCREMENT`), not `UUID`.** The repo
    this service lives in already uses `Long` IDs everywhere (API gateway,
    Feign clients across 9+ other services); switching one service to UUID
    would have required touching all of them. Applies to every entity below.
-   **Database is MySQL (`airline_user`), not PostgreSQL** — matches the rest
    of the repo, one MySQL instance per service.
-   **API paths are unversioned** (`/auth/*`, `/api/users/*`, `/api/roles/*`,
    `/api/permissions/*`), not `/api/v1/*` — matches this repo's existing
    convention, not this doc's.
-   **`Role`/`Permission`/`RolePermission` exist and have full CRUD**, but are
    an *unconsumed, additive* concept — nothing assigns them to a user yet,
    because `AirlineMembership` (section 10.5) doesn't exist. There's also
    **no seed data** — section 23's `V7`–`V9` seed migrations were never
    written; the tables start empty and are populated only through the API.
-   **Account lockout was added beyond this doc's scope**: `User` has
    `status`, `failed_login_attempts`, `locked_until` (migration `V7`, not in
    this doc's original Flyway plan). This is the implemented answer to this
    doc's "rate-limit login attempts" requirement (section 20) — it's
    per-account lockout, not IP-based rate limiting, and there is still no
    rate limiting on verification-email resends.
-   **Error responses have no `code` field** — section 19's suggested shape
    (`VALIDATION_ERROR`, `DUPLICATE_RESOURCE`, etc.) was never implemented;
    the shared `ErrorResponse` (in `common-lib`, used repo-wide) only has
    `{timestamp, status, error, message, path}`.
-   **`GET /api/v1/users/me` / `PATCH /api/v1/users/me` were never built.**
    Instead there's `GET /api/users/profile` (via an `X-User-Email` header,
    the repo's gateway-trust convention — see `CLAUDE.md`) and
    `GET /api/users/{userId}`/`GET /api/users` (list-all — **not** in this
    doc's original API list, and currently has **no authorization check at
    all**, a known, deliberately deferred gap; the plan is to close it from
    `api-gateway` once roles/permissions are wired through).
-   **Role/Permission endpoints ended up as full CRUD, not read-only-and-
    admin-restricted** as section 7 recommended: `POST /api/roles`,
    `POST /api/roles/{roleId}/permissions` (assign), `DELETE .../permissions/
    {permissionId}` (unassign), `POST /api/permissions` all exist, and — like
    the `/api/users` gap above — carry no authorization check yet.
-   **A transactionality lesson worth carrying into the Airline Service**:
    `refresh()`'s reuse-detection revocation, and `login()`'s failed-attempt
    tracking, both write a row and *then* throw to reject the request. Under
    a bare `@Transactional`, Spring's default rollback-on-`RuntimeException`
    silently undoes that write — the mass-revocation and the lockout counter
    both shipped broken this way initially and were only caught by live
    end-to-end testing (a DB check, not code review, found it). Both now use
    `@Transactional(noRollbackFor = UserException.class)`. Anywhere the
    Airline Service does "write a row, then throw to reject" (e.g. recording
    a rejected onboarding review — section 8, step 7) needs the same pattern.
-   **Everything else in sections 8 and 9 (registration/login flow) matches
    this doc closely** — normalized email, password hashing, verification-
    token issuance, account-status/lockout/email-verification checks before
    issuing tokens, refresh-token rotation and hash-at-rest, `last_login`
    update. See the field-level annotations in section 6 for exact naming.

------------------------------------------------------------------------

# 2. High-Level Business Concept

A GDS acts as a platform between airlines and travel sellers or other
authorized users. In this project, the initial focus is on managing
airline organizations and their users.

A person first creates a GDS account. After verifying their email, they
can submit an airline onboarding application. The application is
reviewed by an authorized GDS administrator. If approved, the system
creates the airline and establishes an explicitly selected initial
administrator.

The applicant is **not automatically considered the airline owner**.

The initial administrator must be explicitly selected or confirmed
during the approval process. If the approved user is assigned the
`OWNER` role, that role means:

> The highest administrative authority within the airline organization
> in the GDS platform.

It does not necessarily mean that the person is the airline's legal
owner.

------------------------------------------------------------------------

# 3. Recommended Technology Stack

## Backend

-   Java
-   Spring Boot
-   Spring Security
-   Spring Data JPA
-   Hibernate
-   Jakarta Validation
-   Maven

## Database

-   PostgreSQL *(as originally designed; **User Service actually uses MySQL**,
    matching the rest of this repo — see section 1a)*
-   Flyway for database migrations *(implemented — `services/user-service/
    src/main/resources/db/migration/`, V1–V7, no seed migrations)*

## Authentication

-   JWT access tokens
-   Refresh tokens
-   BCrypt or Argon2 password hashing
-   Email verification

## Storage

-   Cloudflare R2 or another S3-compatible object storage provider
-   Private bucket
-   Short-lived signed download URLs

## Testing

-   JUnit
-   Mockito
-   Testcontainers
-   PostgreSQL integration tests
-   S3-compatible local storage emulator where useful

## Communication

-   REST/JSON initially
-   Kafka or another event broker later, when asynchronous integration
    becomes necessary

------------------------------------------------------------------------

# 4. Service Responsibilities

## 4.1 User Service

The User Service owns identity and global authorization definitions.

Responsibilities:

-   User registration
-   Password hashing
-   Login
-   JWT generation
-   Refresh token management
-   Logout and token revocation
-   Email verification
-   User profile management
-   Roles
-   Permissions
-   Role-permission mappings

The User Service does **not** own airline memberships because
memberships are contextual to a specific airline.

**As implemented**: everything above is done except "user profile
management" is read-only — there's no way to update a user's own profile
(name, phone, etc.) through the API yet, only `GET /api/users/profile`/
`{userId}`/list. See section 7.

## 4.2 Airline Service

The Airline Service owns airline organizations and their relationships
with users.

Responsibilities:

-   Airline onboarding applications
-   Onboarding documents and metadata
-   Review history
-   Airline records
-   Airline memberships
-   Airline invitations
-   Airline-specific access control
-   Airline administration

The Airline Service stores `userId` and `roleId` as logical references
to the User Service. These are not physical database foreign keys
because the services have separate databases.

------------------------------------------------------------------------

# 5. Service and Database Boundaries

## User Service Database

Suggested database name:

``` text
user_db
```

**As implemented**: database is `airline_user` (MySQL 8, `localhost:3306` in
local dev), not `user_db`/PostgreSQL — matches the repo-wide one-MySQL-
instance-per-service convention. See section 1a.

Tables (all implemented, via Flyway V1–V6):

-   `users`
-   `roles`
-   `permissions`
-   `role_permissions`
-   `email_verification_tokens`
-   `refresh_tokens`

Plus `flyway_schema_history` (Flyway's own bookkeeping table) and columns
added by `V7` (`status`, `failed_login_attempts`, `locked_until` on `users`
— account lockout, not in this doc's original scope, see section 1a).

## Airline Service Database

Suggested database name:

``` text
airline_db
```

Tables:

-   `airlines`
-   `airline_onboarding_applications`
-   `onboarding_documents`
-   `onboarding_reviews`
-   `airline_memberships`
-   `airline_invitations`

The User Service and Airline Service should not directly query each
other's databases.

------------------------------------------------------------------------

# 6. User Service Entities

## 6.1 User

Table:

``` text
users
```

### Purpose

Represents a person who can log in to the GDS.

A single user account can belong to multiple airlines and can have a
different role in each airline.

### Fields — as implemented

`services/user-service/src/main/java/com/sunday/services/model/User.java`,
migrations `V1__create_users.sql` + `V7__add_user_status_and_lockout.sql`.

  ------------------------------------------------------------------------------------
  Field                     Actual type              Notes
  ------------------------- ------------------------ -----------------------------------
  `id`                      `BIGINT` (Long, IDENTITY) not UUID — see section 1a

  `email`                   `VARCHAR(255)`, unique    normalized (trimmed + lowercased)
                                                       before every save/lookup

  `password`                `VARCHAR(255)`            not renamed to `password_hash`;
                                                       BCrypt hash

  `first_name`,             `VARCHAR(100)`,           `middle_name` optional
  `last_name`, `middle_name` `VARCHAR(100)`, nullable

  `phone_number`            `VARCHAR(30)`, nullable

  `status`                  `VARCHAR(20)`            enum `ACTIVE`/`SUSPENDED`/`LOCKED`
                                                       (see note below — differs from the
                                                       doc's suggested values)

  `failed_login_attempts`   `INT`, default 0          not in original design; tracks
                                                       consecutive bad-password attempts

  `locked_until`            `DATETIME`, nullable      set when `failed_login_attempts`
                                                       reaches `auth.max-failed-login-
                                                       attempts` (config, default 5);
                                                       cleared on successful login

  `email_verified`          `BOOLEAN`, default false  **enforced as a login gate** — see
                                                       section 1a; not a soft flag

  `last_login`              `DATETIME`, nullable      not renamed to `last_login_at`

  `created_at`, `updated_at` `DATETIME`               unchanged from design
  ------------------------------------------------------------------------------------

### Original (aspirational) fields

  --------------------------------------------------------------------------
  Field              Suggested Type                Required Description
  ------------------ ---------------- --------------------- ----------------
  `id`               UUID                               Yes Unique user
                                                            identifier

  `email`            VARCHAR(255)                       Yes Login email;
                                                            must be unique

  `password_hash`    VARCHAR(255)                       Yes Securely hashed
                                                            password

  `first_name`       VARCHAR(100)                       Yes User's first
                                                            name

  `last_name`        VARCHAR(100)                       Yes User's surname

  `middle_name`      VARCHAR(100)                        No Optional middle
                                                            name

  `phone_number`     VARCHAR(30)                         No Optional phone
                                                            number

  `status`           ENUM                               Yes Account status

  `email_verified`   BOOLEAN                            Yes Whether email
                                                            verification is
                                                            complete

  `last_login_at`    TIMESTAMP                           No Most recent
                                                            successful login

  `created_at`       TIMESTAMP                          Yes Creation
                                                            timestamp

  `updated_at`       TIMESTAMP                          Yes Last update
                                                            timestamp
  --------------------------------------------------------------------------

### Suggested User Status

``` text
ACTIVE
SUSPENDED
DISABLED
```

**As implemented** (`com.sunday.services.enums.UserStatus`): `ACTIVE`,
`SUSPENDED`, `LOCKED` — `LOCKED` replaces `DISABLED` and is set automatically
by the account-lockout mechanism (not manually, unlike `SUSPENDED`); a
locked account is distinct from one with a non-null `locked_until` — the
lockout timer (`locked_until`) is what actually gates login, `status` is a
separate, currently-manual field nothing sets to `LOCKED` automatically yet.

### Design Notes

-   Normalize email addresses before storing them.
-   Add a unique constraint on normalized email.
-   Never store raw passwords.
-   UUIDs help avoid exposing sequential database IDs.
-   Avoid hard deletion where audit history or business relationships
    must be retained.

------------------------------------------------------------------------

## 6.2 Role

Table:

``` text
roles
```

### Purpose

Defines a reusable role that can be assigned to a user within an airline
membership.

A role should describe a level of responsibility, while permissions
describe what the role can do.

### Fields

  Field           Suggested Type     Required Description
  --------------- ---------------- ---------- ----------------------------
  `id`            UUID                    Yes Unique role identifier
  `name`          VARCHAR(100)            Yes Role name
  `description`   VARCHAR(500)             No Explanation of the role
  `status`        ENUM                    Yes Whether the role is active
  `created_at`    TIMESTAMP               Yes Creation timestamp
  `updated_at`    TIMESTAMP               Yes Last update timestamp

### Suggested Role Status

``` text
ACTIVE
INACTIVE
```

### Initial Roles

``` text
OWNER
ADMIN
OPERATIONS_MANAGER
BOOKING_AGENT
VIEWER
```

**As implemented**: `id` is `Long`, not UUID (as throughout — section 1a).
**None of these initial roles are seeded** — the `V7`–`V9` seed migrations
this doc's Flyway plan (section 23) called for were never written, so the
`roles` table starts empty and these five values must be created manually
via `POST /api/roles` if/when needed. Full CRUD exists (`GET/POST /api/roles`,
`GET /api/roles/{roleId}`), beyond this section's original read-only scope.

**Update (2026-09-18) — role scope, and only 3 of the 5 roles built**: `Role`
gained a `scope` field (`PLATFORM`/`AIRLINE`, migration `V8`, default
`AIRLINE`), to resolve a gap this doc didn't originally address: reviewing
onboarding applications (section 13 step 7, section 20: *"Restrict onboarding
review to authorized GDS administrators"*) needs a **platform-level** role
that exists *before* any airline (and therefore any `AirlineMembership`) does
— the doc's `OWNER`/`ADMIN`/`OPERATIONS_MANAGER`/`BOOKING_AGENT`/`VIEWER` are
all `AIRLINE`-scoped by contrast, meant to be granted via `AirlineMembership`
once it exists. A `GDS_ADMIN` (`PLATFORM`-scoped) role has been created for
this, with its own direct-grant mechanism (`UserPlatformRole`, migration
`V9`, `POST/DELETE /api/roles/{roleId}/users/{userId}`, `GET /api/users/
{userId}/roles`) since there's no membership to hang a platform role off of.
That endpoint rejects `AIRLINE`-scoped roles (`403`) — they must go through
`AirlineMembership` once that's built, not this shortcut.

Only `OWNER`, `ADMIN`, `VIEWER` were actually created — `OPERATIONS_MANAGER`
and `BOOKING_AGENT` name capabilities (flight ops, booking) this phase
explicitly excludes (section 1), so there's nothing for them to govern yet;
add them later alongside those capabilities. `ADMIN` and `OWNER` are not
identical: `OWNER` has one exclusive permission (`MEMBER_REMOVE`, see section
6.3's updated permission list) so the distinction is real, not just nominal.

A real seed set (not Flyway, not throwaway test data) now exists in the dev
DB: `GDS_ADMIN`/`OWNER`/`ADMIN`/`VIEWER` plus the permission set in section
6.3's update, wired together exactly as the table there shows.

### Design Notes

Roles are defined globally in the User Service but applied to users
within an airline through `AirlineMembership`.

For example:

-   John can be `OWNER` in ABC Airways.
-   John can be `VIEWER` in XYZ Airways.
-   Mary can be `ADMIN` in ABC Airways.

------------------------------------------------------------------------

## 6.3 Permission

Table:

``` text
permissions
```

### Purpose

Defines a specific action that a user may perform.

Permissions allow fine-grained authorization instead of relying only on
role names.

### Fields

  Field           Suggested Type     Required Description
  --------------- ---------------- ---------- -------------------------------
  `id`            UUID                    Yes Unique permission identifier
  `name`          VARCHAR(150)            Yes Permission name
  `description`   VARCHAR(500)             No Explanation of the permission
  `created_at`    TIMESTAMP               Yes Creation timestamp
  `updated_at`    TIMESTAMP               Yes Last update timestamp

### Initial Permissions

``` text
AIRLINE_READ
AIRLINE_UPDATE
MEMBER_READ
MEMBER_INVITE
MEMBER_UPDATE
MEMBER_REMOVE
USER_READ
USER_UPDATE
ONBOARDING_READ
ONBOARDING_REVIEW
```

**As implemented**: `id` is `Long`, not UUID. **Not seeded** — same as
`roles` above, the table starts empty. Full CRUD exists (`GET/POST
/api/permissions`, `GET /api/permissions/{permissionId}/roles`), beyond
this section's original read-only scope.

**Update (2026-09-18) — granular permission set actually created**, replacing
`MEMBER_UPDATE` with two finer-grained permissions and dropping `USER_UPDATE`
(no capability anywhere in the design flows needs it, and self-profile-edit
isn't even built — see section 7's `PATCH /users/me` note). Actual set:
`ONBOARDING_READ`, `ONBOARDING_REVIEW` (`PLATFORM`-scoped, `GDS_ADMIN` only —
see section 6.2's update), `AIRLINE_READ`, `AIRLINE_UPDATE`, `MEMBER_READ`,
`MEMBER_INVITE`, `MEMBER_UPDATE_ROLE`, `MEMBER_UPDATE_STATUS`, `MEMBER_REMOVE`,
`USER_READ` (`AIRLINE`-scoped). Role → permission mapping actually created:

| Role | Permissions |
|---|---|
| `GDS_ADMIN` | `ONBOARDING_READ`, `ONBOARDING_REVIEW` |
| `VIEWER` | `AIRLINE_READ`, `MEMBER_READ`, `USER_READ` |
| `ADMIN` | `AIRLINE_READ`, `AIRLINE_UPDATE`, `MEMBER_READ`, `MEMBER_INVITE`, `MEMBER_UPDATE_ROLE`, `MEMBER_UPDATE_STATUS`, `USER_READ` |
| `OWNER` | everything `ADMIN` has, plus `MEMBER_REMOVE` |

### Design Notes

Permissions should be stable identifiers used by application
authorization logic.

For example:

-   `MEMBER_INVITE` allows inviting users.
-   `MEMBER_UPDATE` allows changing membership roles or status.
-   `ONBOARDING_REVIEW` allows an authorized GDS administrator to review
    applications.

------------------------------------------------------------------------

## 6.4 RolePermission

Table:

``` text
role_permissions
```

### Purpose

Maps roles to permissions.

This is a many-to-many relationship:

-   One role can have many permissions.
-   One permission can belong to many roles.

### Fields

  Field             Suggested Type     Required Description
  ----------------- ---------------- ---------- -------------------------------
  `role_id`         UUID                    Yes Logical reference to the role
  `permission_id`   UUID                    Yes Reference to the permission

### Constraints

``` text
UNIQUE(role_id, permission_id)
```

### Example

The `ADMIN` role may have:

``` text
AIRLINE_READ
AIRLINE_UPDATE
MEMBER_READ
MEMBER_INVITE
MEMBER_UPDATE
MEMBER_REMOVE
USER_READ
USER_UPDATE
```

### Design Notes

Role and permission data can initially be inserted using Flyway seed
migrations.

**As implemented**: `id` is `Long`. `role_id`/`permission_id` are real JPA
`@ManyToOne` foreign keys (not "logical references" — `Role`, `Permission`,
and `RolePermission` all live in the same `airline_user` database, so a
physical FK applies here, unlike the genuinely cross-service `user_id`/
`role_id` references the Airline Service will need). No seed data — see
above. Full assign/unassign API exists: `POST /api/roles/{roleId}/
permissions` (idempotent — re-assigning an already-assigned permission is a
no-op, not an error) and `DELETE /api/roles/{roleId}/permissions/
{permissionId}`.

------------------------------------------------------------------------

## 6.5 EmailVerificationToken

Table:

``` text
email_verification_tokens
```

### Purpose

Allows the system to verify that a user controls the email address used
during registration.

### Fields

  Field          Suggested Type     Required Description
  -------------- ---------------- ---------- --------------------------------
  `id`           UUID                    Yes Token record identifier
  `user_id`      UUID                    Yes User associated with the token
  `token_hash`   VARCHAR(255)            Yes Hash of the verification token
  `expires_at`   TIMESTAMP               Yes Expiration timestamp
  `used_at`      TIMESTAMP                No When the token was consumed
  `created_at`   TIMESTAMP               Yes Creation timestamp

### Verification Flow

1.  User submits registration details.
2.  System validates the request.
3.  Password is hashed.
4.  User is created.
5.  A random verification token is generated.
6.  Only the token hash is stored.
7.  The raw token is sent by email.
8.  User opens the verification link.
9.  The submitted token is hashed and compared.
10. The token's expiration and used status are checked.
11. `email_verified` is set to `true`.
12. The token is marked as used.

### Security Notes

-   Do not store the raw token.
-   Make tokens short-lived.
-   Prevent token reuse.
-   Rate-limit verification email resends.
-   Do not reveal whether a specific email exists during resend
    requests.

### As implemented

`id`/`user_id` are `Long` (FK to `users`, `@ManyToOne`). Flow matches this
section closely: raw token is two concatenated random UUIDs, hashed with
plain SHA-256 (deterministic — needed for exact-match lookup by hash;
contrast with BCrypt's salted password hashing, which can't be looked up
this way) before storage; default TTL 24h (`auth.verification-token-ttl-
hours`, configurable). **Resending invalidates any still-outstanding unused
token first**, so at most one is ever live per user (not explicitly called
for in this doc, added after a review flagged unlimited simultaneously-valid
tokens as a gap). `resendVerification` returns an identical response
regardless of whether the email exists, is unverified, or already verified
— matches "do not reveal" above. **Not implemented**: rate-limiting resends
(the "rate-limit" bullet above) — there's no throttle on how often
`/auth/resend-verification` can be called for a given email today. Expired/
used rows are purged by a daily scheduled job (`TokenCleanupScheduler`, not
in this doc's original design), not left to accumulate forever.
Endpoint is both `GET /auth/verify-email?token=...` (this doc's suggestion,
kept for email-link compatibility) and `POST /auth/verify-email` (token in
the request body — mitigates the token appearing in proxy/access logs and
browser history that a GET query param invites).

------------------------------------------------------------------------

## 6.6 RefreshToken

Table:

``` text
refresh_tokens
```

### Purpose

Allows users to obtain new access tokens without logging in again.

It also supports session-level revocation, such as logging out from one
device.

### Fields

  Field            Suggested Type     Required Description
  ---------------- ---------------- ---------- ---------------------------------
  `id`             UUID                    Yes Refresh token record identifier
  `user_id`        UUID                    Yes User associated with the token
  `token_hash`     VARCHAR(255)            Yes Hash of the refresh token
  `expires_at`     TIMESTAMP               Yes Expiration timestamp
  `revoked_at`     TIMESTAMP                No Revocation timestamp
  `last_used_at`   TIMESTAMP                No Last usage timestamp
  `user_agent`     VARCHAR(500)             No Device or browser information
  `ip_address`     VARCHAR(100)             No Login or usage IP address
  `created_at`     TIMESTAMP               Yes Creation timestamp

### Design Notes

-   Store only a hash of the refresh token.
-   Support refresh-token rotation.
-   Revoke tokens on logout.
-   Consider revoking the entire token family if token reuse is
    detected.
-   Do not log tokens.

### As implemented

`id`/`user_id` are `Long`. Same SHA-256-hash-at-rest scheme as the
verification token above. Default TTL 30 days (`auth.refresh-token-ttl-
days`, configurable). `user_agent`/`ip_address` **are** populated (extracted
from the `User-Agent` header and `X-Forwarded-For`/remote-addr on each
`login`/`refresh` call) — worth calling out since it'd be easy to add these
columns and forget to actually fill them in, which is exactly what happened
during initial implementation before a review caught it.

**Reuse detection uses the blunt version of "revoke the entire token
family"**: there's no token-family/chain concept — presenting an
already-revoked refresh token revokes *every* currently-active refresh
token for that user (simpler than family tracking, same practical effect
for a single-device-compromise scenario). This is implemented as a
write-then-throw operation inside `refresh()` and needs
`@Transactional(noRollbackFor = UserException.class)` to actually persist
the revocation — see section 1a's transactionality note; this exact bug
shipped once and was only caught by live testing.

Expired rows are purged by the same daily `TokenCleanupScheduler` mentioned
above (not in this doc's original design).

------------------------------------------------------------------------

# 7. User Service APIs

## As implemented (actual paths — unversioned, not `/api/v1/*`; see section 1a)

``` http
POST   /auth/signup                                    (not "register")
POST   /auth/login
POST   /auth/refresh
POST   /auth/logout
GET    /auth/verify-email?token=...
POST   /auth/verify-email                               (token in body — extra, not in original design)
POST   /auth/resend-verification

GET    /api/users/profile                                (X-User-Email header, not "/me")
GET    /api/users/{userId}
GET    /api/users                                         (list all — extra, not in original design)

GET    /api/roles
GET    /api/roles/{roleId}
POST   /api/roles                                         (extra — original design was read-only)
GET    /api/roles/{roleId}/permissions                    (extra)
POST   /api/roles/{roleId}/permissions                    (assign — extra)
DELETE /api/roles/{roleId}/permissions/{permissionId}     (unassign — extra)

GET    /api/permissions
POST   /api/permissions                                   (extra — original design was read-only)
GET    /api/permissions/{permissionId}/roles              (extra)
```

**No `PATCH /api/users/me`-equivalent exists** — there is currently no way
to update a user's own profile (name, phone number, etc.) through the API.

**None of these endpoints have any authorization check** — `SecurityConfig`
currently `permitAll()`s everything at this service, relying entirely on
network-level trust (only the gateway should be able to reach it) rather
than per-route checks. This is a known, deliberately deferred gap for
`GET /api/users*` and all of `/api/roles*`/`/api/permissions*` — the design
below's recommendation to restrict role/permission management to trusted
admins has **not** been implemented. The plan is to close this from
`api-gateway` as a single RBAC entry point once roles/permissions are wired
through to `AirlineMembership` (which doesn't exist yet) — see `CLAUDE.md`.

## Original design (aspirational, superseded by the above)

## Authentication APIs

``` http
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/logout
GET  /api/v1/auth/verify-email
POST /api/v1/auth/resend-verification
```

## User APIs

``` http
GET   /api/v1/users/me
PATCH /api/v1/users/me
```

## Role and Permission APIs

``` http
GET /api/v1/roles
GET /api/v1/roles/{roleId}
GET /api/v1/permissions
```

Role and permission management endpoints should be restricted to trusted
system administrators if they are exposed at all. In many systems, roles
and permissions are managed through migrations and internal
administration rather than public endpoints.

------------------------------------------------------------------------

# 8. User Registration Flow

## Step-by-Step

1.  The user submits:

    -   Email
    -   Password
    -   First name
    -   Last name
    -   Optional middle name
    -   Optional phone number

2.  The User Service validates:

    -   Email format
    -   Password strength
    -   Required fields
    -   Duplicate email
    -   Input length

3.  The password is hashed using BCrypt or Argon2.

4.  The user record is created with:

    -   `status = ACTIVE`
    -   `email_verified = false`

5.  A verification token is generated and stored as a hash.

6.  A verification email is sent.

7.  The user verifies their email.

8.  The user can now log in, subject to account status and application
    rules.

### As implemented

Matches closely, with one hardening beyond this doc: **registration issues
no tokens at all** (not even implicitly) — the response is just the created
user's public fields plus a "check your email" message. Login is fully
blocked (`EMAIL_NOT_VERIFIED`) until the token is verified; there's no
"log in but with reduced capability" middle ground. Password minimum length
is enforced (8 characters) via Jakarta Bean Validation on the request DTO,
alongside the required-field/email-format checks this doc calls for.
Duplicate-email is checked both up front (fast path) and via a
`DataIntegrityViolationException` catch around the actual insert (closes
the race where two concurrent signups for the same email both pass the
up-front check) — confirmed with 5 concurrent requests for the same new
email producing exactly one success and four clean rejections, never a 500.

## Transaction Boundary

The creation of the user and verification-token record should occur in
one local database transaction.

Email delivery should ideally use an outbox or reliable asynchronous
mechanism so that a temporary email provider failure does not corrupt
the registration transaction.

------------------------------------------------------------------------

# 9. Login Flow

## Step-by-Step

1.  User submits email and password.
2.  System normalizes the email.
3.  User is located.
4.  Account status is checked.
5.  Password is verified.
6.  Email verification requirements are checked.
7.  An access token is generated.
8.  A refresh token is generated.
9.  The refresh token hash is stored.
10. `last_login_at` is updated.
11. Tokens are returned to the client.

## Recommended Initial JWT Claims

``` text
sub
email
iat
exp
```

Do not place every airline membership and permission into the JWT
initially because memberships and roles can change frequently. Instead,
resolve airline membership and permissions when processing
airline-scoped requests.

### Update (2026-09-20) — this guidance was tested and confirmed correct

`PLATFORM`-scoped roles (`UserPlatformRole`, section 6.2's update) *are* now
placed in the JWT `authorities` claim — that's fine, since a platform role
like `GDS_ADMIN` isn't scoped to any resource, so there's nothing for it to
go stale against.

`AIRLINE`-scoped roles (via `AirlineMembership`) were a different story: a
synchronous Feign call and an async Kafka-mirror from `airline-core-service`
into a local `user-service` table were both designed as ways to fold them
into the same JWT claim. The mirror was actually built, end-to-end, then
deliberately reverted — building it surfaced the exact problem this
section warned about, plus one more: a flat `GrantedAuthority` string has
no room for *which* airline a role applies to, so at best it could only
ever express "holds role X on some airline somewhere," which is not a
question any real authorization decision in this system actually asks —
every airline-scoped decision needs "on airline N specifically." Real
per-airline authorization stays exactly where this section implies it
should: a live lookup against `AirlineMembership` at request time
(`AirlineServiceImpl.requireActiveMembership` in `airline-core-service`),
never something cached in a token. See `CLAUDE.md`'s `user-service`
section for the fuller writeup.

### As implemented

Login flow matches this section's steps closely, including the ordering
(locked/status checks happen *before* password verification — see the
account-lockout note in section 1a about that revealing lock state to an
unauthenticated caller, an accepted tradeoff). Account status now covers
both `User.status` and the `locked_until` timestamp (section 6.1 — beyond
this doc's original design). JWT claims implemented: `jti` (random UUID,
for future per-token revocation — not currently used for anything, added
speculatively), `sub` (=email), `iat`, `exp`, `email`, `authorities`
(comma-joined granted authorities — populated from a user's `PLATFORM`-
scoped `UserPlatformRole` grants as `ROLE_<name>`; see the "Recommended
Initial JWT Claims" update above for why `AIRLINE`-scoped roles are
deliberately excluded), `userId`. Access tokens
are short-lived (15 min default, `jwt.access-token-ttl-minutes`) precisely
so the refresh-token flow in section 6.6 is meaningful, per this doc's own
"short-lived access tokens" security requirement (section 20).
Wrong-password and unknown-email both return the *identical* generic
"Invalid email or password" (via a timing-equalized dummy-hash comparison
on the not-found path) — this doc doesn't call this out explicitly, but it
closes an account-enumeration oracle that an earlier implementation had
(distinct error shapes/status codes for the two cases).

------------------------------------------------------------------------

# 10. Airline Service Entities

## 10.1 Airline

Table:

``` text
airlines
```

### Purpose

Represents an airline organization that has been approved and created on
the platform.

### Fields

  -------------------------------------------------------------------------------
  Field                   Suggested Type                Required Description
  ----------------------- ---------------- --------------------- ----------------
  `id`                    UUID                               Yes Unique airline
                                                                 identifier

  `legal_name`            VARCHAR(255)                       Yes Registered legal
                                                                 business name

  `display_name`          VARCHAR(255)                       Yes Public or
                                                                 operational name

  `iata_code`             VARCHAR(2)                          No Externally
                                                                 assigned IATA
                                                                 code

  `icao_code`             VARCHAR(3)                          No Externally
                                                                 assigned ICAO
                                                                 code

  `country_code`          VARCHAR(2)                         Yes Country code

  `registration_number`   VARCHAR(100)                       Yes Government or
                                                                 business
                                                                 registration
                                                                 number

  `status`                ENUM                               Yes Airline account
                                                                 status

  `created_at`            TIMESTAMP                          Yes Creation
                                                                 timestamp

  `updated_at`            TIMESTAMP                          Yes Last update
                                                                 timestamp
  -------------------------------------------------------------------------------

### Suggested Airline Status

``` text
ACTIVE
SUSPENDED
INACTIVE
```

### Design Notes

-   IATA and ICAO codes are assigned by external aviation authorities or
    industry bodies.
-   The GDS must not generate these codes.
-   Codes can be optional during early onboarding if the business policy
    permits it.
-   Add unique constraints to non-null IATA and ICAO codes.
-   Validate the format of codes.
-   Country codes should use a consistent standard such as ISO 3166-1
    alpha-2.

------------------------------------------------------------------------

## 10.2 AirlineOnboardingApplication

Table:

``` text
airline_onboarding_applications
```

### Purpose

Represents an application submitted by a user or organization to
register an airline on the GDS.

It separates the application process from the final approved airline
record.

### Fields

  ---------------------------------------------------------------------------------
  Field                     Suggested Type                Required Description
  ------------------------- ---------------- --------------------- ----------------
  `id`                      UUID                               Yes Application
                                                                   identifier

  `applicant_user_id`       UUID                               Yes User who
                                                                   submitted the
                                                                   application

  `initial_admin_user_id`   UUID                                No Explicitly
                                                                   selected initial
                                                                   administrator

  `legal_name`              VARCHAR(255)                       Yes Proposed legal
                                                                   airline name

  `display_name`            VARCHAR(255)                       Yes Proposed display
                                                                   name

  `iata_code`               VARCHAR(2)                          No Proposed IATA
                                                                   code

  `icao_code`               VARCHAR(3)                          No Proposed ICAO
                                                                   code

  `country_code`            VARCHAR(2)                         Yes Airline country

  `registration_number`     VARCHAR(100)                       Yes Registration
                                                                   number

  `status`                  ENUM                               Yes Current
                                                                   onboarding
                                                                   status

  `rejection_reason`        TEXT                                No Reason for
                                                                   rejection

  `submitted_at`            TIMESTAMP                           No Submission
                                                                   timestamp

  `reviewed_at`             TIMESTAMP                           No Review
                                                                   completion
                                                                   timestamp

  `created_at`              TIMESTAMP                          Yes Creation
                                                                   timestamp

  `updated_at`              TIMESTAMP                          Yes Last update
                                                                   timestamp
  ---------------------------------------------------------------------------------

### Suggested Onboarding Status

``` text
DRAFT
SUBMITTED
UNDER_REVIEW
APPROVED
REJECTED
WITHDRAWN
```

### Important Rule

The applicant is not automatically made the owner.

The `initial_admin_user_id` should be explicitly selected or confirmed
during the review and approval process. The system may assign that user
the `OWNER` role only after approval.

### As implemented

`country_code` stays as free-text `country` instead (a known, out-of-scope
divergence — see `CLAUDE.md`'s `airline-core-service` section). The four
fields this section's own field table marks `Required: Yes` (`legal_name`,
`display_name`, `country`, `registration_number`) are enforced by Bean
Validation on the same DTO used for both draft creation and updates —
but only on *creation* (`OnCreate` validation group), not on update,
since update is a PATCH and Jackson can't tell "field omitted" from
"field explicitly null." A separate, entity-level completeness check
(`requireCompleteForSubmission`) still runs at submission — it's the
backstop against a later PATCH blanking one of those fields to `""` after
creation, which DTO validation alone can't safely close without breaking
legitimate partial updates.

------------------------------------------------------------------------

## 10.3 OnboardingDocument

Table:

``` text
onboarding_documents
```

### Purpose

Stores metadata about documents submitted during airline onboarding.

The actual file should be stored in object storage, not directly inside
PostgreSQL.

### Fields

  ------------------------------------------------------------------------------
  Field                  Suggested Type                Required Description
  ---------------------- ---------------- --------------------- ----------------
  `id`                   UUID                               Yes Document
                                                                identifier

  `application_id`       UUID                               Yes Related
                                                                onboarding
                                                                application

  `document_type`        ENUM                               Yes Type of document

  `original_file_name`   VARCHAR(255)                       Yes Original
                                                                uploaded
                                                                filename

  `storage_provider`     VARCHAR(50)                        Yes Storage
                                                                provider,
                                                                e.g. R2

  `storage_bucket`       VARCHAR(255)                       Yes Storage bucket
                                                                name

  `storage_key`          VARCHAR(500)                       Yes Object storage
                                                                key

  `content_type`         VARCHAR(150)                       Yes MIME type

  `file_size`            BIGINT                             Yes File size in
                                                                bytes

  `checksum`             VARCHAR(255)                        No File integrity
                                                                checksum

  `status`               ENUM                               Yes Document review
                                                                status

  `uploaded_by`          UUID                               Yes User who
                                                                uploaded the
                                                                document

  `verified_by`          UUID                                No Reviewer who
                                                                verified the
                                                                document

  `verified_at`          TIMESTAMP                           No Verification
                                                                timestamp

  `rejection_reason`     TEXT                                No Reason for
                                                                document
                                                                rejection

  `created_at`           TIMESTAMP                          Yes Creation
                                                                timestamp

  `updated_at`           TIMESTAMP                          Yes Last update
                                                                timestamp
  ------------------------------------------------------------------------------

### Suggested Document Types

``` text
CERTIFICATE_OF_INCORPORATION
AIR_OPERATOR_CERTIFICATE
OPERATING_LICENSE
BUSINESS_REGISTRATION
OTHER
```

### Suggested Document Status

``` text
PENDING
VERIFIED
REJECTED
```

### Design Notes

The database should store metadata and the storage location. The actual
binary file should remain in private object storage.

------------------------------------------------------------------------

## 10.4 OnboardingReview

Table:

``` text
onboarding_reviews
```

### Purpose

Stores the review history for an onboarding application.

Review history should be append-only rather than overwritten so that the
system retains an audit trail.

### Fields

  Field                Suggested Type     Required Description
  -------------------- ---------------- ---------- -------------------------------
  `id`                 UUID                    Yes Review identifier
  `application_id`     UUID                    Yes Application being reviewed
  `reviewer_user_id`   UUID                    Yes User who performed the review
  `decision`           ENUM                    Yes Review decision
  `comments`           TEXT                     No Review notes
  `created_at`         TIMESTAMP               Yes Review timestamp

### Suggested Review Decisions

``` text
APPROVED
REJECTED
REQUESTED_CHANGES
```

### Design Notes

Multiple review records may exist for one application. This preserves
the full decision history and supports compliance and dispute
resolution.

### As implemented

`decision` is a real `ReviewDecision` enum with exactly these three
constants — not a `String` the service manually parses. It lives in
`common-lib` rather than `airline-core-service` specifically so the
shared request DTO (`OnboardingReviewRequest`) can be typed against it
directly: `common-lib` can't depend on a type living in a downstream
service module, so the enum had to move there first. An illegal value now
fails at JSON deserialization, before the controller method even runs;
`GlobalExceptionHandler` (common-lib, repo-wide) gained a generic handler
for that case so the client still gets the app's normal error shape
(listing the legal values) instead of Spring Boot's default error body.

------------------------------------------------------------------------

## 10.5 AirlineMembership

Table:

``` text
airline_memberships
```

### Purpose

Represents a user's membership in a specific airline organization.

This is the central entity that supports multi-airline access.

### Fields

  Field          Suggested Type     Required Description
  -------------- ---------------- ---------- ----------------------------------------
  `id`           UUID                    Yes Membership identifier
  `user_id`      UUID                    Yes Logical reference to User Service user
  `airline_id`   UUID                    Yes Airline the user belongs to
  `role_id`      UUID                    Yes Logical reference to User Service role
  `status`       ENUM                    Yes Membership status
  `joined_at`    TIMESTAMP                No When membership became active
  `created_at`   TIMESTAMP               Yes Creation timestamp
  `updated_at`   TIMESTAMP               Yes Last update timestamp

### Suggested Membership Status

``` text
INVITED
ACTIVE
SUSPENDED
REMOVED
```

### Constraints

``` text
UNIQUE(user_id, airline_id)
```

### Design Notes

-   Do not put `userId` directly on the `Airline` entity.
-   A user may belong to many airlines.
-   An airline may have many users.
-   A user should generally have one active membership per airline, with
    one current role.
-   `user_id` and `role_id` are logical cross-service references, not
    physical foreign keys.

------------------------------------------------------------------------

## 10.6 AirlineInvitation

Table:

``` text
airline_invitations
```

### Purpose

Allows an airline administrator to invite an employee or other user to
join an airline.

The invitee may not yet have a GDS account, so the invitation is
email-based.

### Fields

  Field           Suggested Type     Required Description
  --------------- ---------------- ---------- ----------------------------------------
  `id`            UUID                    Yes Invitation identifier
  `airline_id`    UUID                    Yes Airline sending the invitation
  `email`         VARCHAR(255)            Yes Email address of invitee
  `role_id`       UUID                    Yes Logical reference to the assigned role
  `invited_by`    UUID                    Yes User who created the invitation
  `token_hash`    VARCHAR(255)            Yes Hash of invitation token
  `status`        ENUM                    Yes Invitation status
  `expires_at`    TIMESTAMP               Yes Invitation expiration
  `accepted_at`   TIMESTAMP                No Acceptance timestamp
  `created_at`    TIMESTAMP               Yes Creation timestamp
  `updated_at`    TIMESTAMP               Yes Last update timestamp

### Suggested Invitation Status

``` text
PENDING
ACCEPTED
EXPIRED
REVOKED
```

### Design Notes

-   Store only a hash of the invitation token.
-   Invitations should expire.
-   Validate that the inviter has permission to invite users.
-   If the invitee already has an account, link the invitation to that
    account after acceptance.
-   If the invitee does not have an account, require registration before
    membership activation.

------------------------------------------------------------------------

# 11. Airline Service APIs

## Onboarding APIs

``` http
POST  /api/v1/onboarding/applications
GET   /api/v1/onboarding/applications/{applicationId}
PATCH /api/v1/onboarding/applications/{applicationId}
POST  /api/v1/onboarding/applications/{applicationId}/submit
```

## Document APIs

``` http
POST /api/v1/onboarding/applications/{applicationId}/documents
GET  /api/v1/onboarding/applications/{applicationId}/documents
GET  /api/v1/onboarding/documents/{documentId}
GET  /api/v1/onboarding/documents/{documentId}/download
```

## Review APIs

``` http
GET  /api/v1/admin/onboarding/applications
GET  /api/v1/admin/onboarding/applications/{applicationId}
POST /api/v1/admin/onboarding/applications/{applicationId}/review
```

## Airline APIs

``` http
GET   /api/v1/airlines/{airlineId}
PATCH /api/v1/airlines/{airlineId}
```

## Membership APIs

``` http
GET   /api/v1/airlines/{airlineId}/members
GET   /api/v1/airlines/{airlineId}/members/{membershipId}
PATCH /api/v1/airlines/{airlineId}/members/{membershipId}
DELETE /api/v1/airlines/{airlineId}/members/{membershipId}
```

## Invitation APIs

``` http
POST /api/v1/airlines/{airlineId}/invitations
GET  /api/v1/airlines/{airlineId}/invitations
POST /api/v1/invitations/{invitationId}/accept
POST /api/v1/invitations/{invitationId}/revoke
```

------------------------------------------------------------------------

# 12. End-to-End Business Flow

## Overall Flow

``` text
User Registration
        |
        v
Email Verification
        |
        v
Login
        |
        v
Create Airline Onboarding Draft
        |
        v
Upload Required Documents
        |
        v
Submit Application
        |
        v
GDS Administrator Review
        |
        +--------------------+
        |                    |
        v                    v
Request Changes         Reject
        |
        v
Resubmission
        |
        v
Approval
        |
        v
Create Airline
        |
        v
Explicitly Establish Initial Administrator
        |
        v
Create OWNER Membership
        |
        v
Invite Employees
        |
        v
Employees Accept Invitations
        |
        v
Create Airline Memberships
```

------------------------------------------------------------------------

# 13. Detailed Registration and Onboarding Flow

## Step 1: Register

The user submits registration information to the User Service.

The User Service:

-   Validates the request.
-   Checks for duplicate email.
-   Hashes the password.
-   Creates the user.
-   Creates an email verification token.
-   Sends a verification email.

## Step 2: Verify Email

The user clicks the verification link.

The User Service:

-   Validates the token.
-   Checks expiration.
-   Checks whether it has already been used.
-   Marks the user email as verified.
-   Marks the token as used.

## Step 3: Login

The user logs in and receives:

-   Access token
-   Refresh token

The access token is used to authenticate later API requests.

## Step 4: Create Onboarding Draft

The user creates an onboarding draft with:

-   Legal name
-   Display name
-   Country
-   Registration number
-   Optional IATA code
-   Optional ICAO code
-   Optional initial administrator information

The application begins in:

``` text
DRAFT
```

A draft allows the user to save incomplete information before
submission.

## Step 5: Upload Documents

The user uploads the required documents.

The Airline Service should:

1.  Authenticate the user.
2.  Confirm the user can modify the application.
3.  Confirm the application is in a valid status.
4.  Validate file size.
5.  Validate MIME type.
6.  Validate file extension.
7.  Validate file signature or magic bytes.
8.  Optionally scan for malware.
9.  Generate a safe object key.
10. Upload the file to private object storage.
11. Save document metadata in PostgreSQL.

## Step 6: Submit Application

Before submission, the system validates:

-   Required application fields
-   Required documents
-   Valid document statuses
-   Valid application state

The application changes from:

``` text
DRAFT -> SUBMITTED
```

The submission timestamp is stored.

## Step 7: Administrative Review

A GDS administrator reviews:

-   Submitted airline information
-   Registration details
-   Uploaded documents
-   Existing airline conflicts
-   IATA and ICAO information, if provided
-   Proposed initial administrator

The reviewer can:

-   Approve
-   Reject
-   Request changes

If changes are requested, the applicant updates the application and
resubmits it.

## Step 8: Approval

Approval should be performed in one local Airline Service database
transaction.

The transaction should:

1.  Confirm the application is eligible for approval.
2.  Confirm required review conditions are met.
3.  Validate the initial administrator.
4.  Create the `Airline` record.
5.  Create an `AirlineMembership` for the initial administrator.
6.  Assign the `OWNER` role.
7.  Set membership status to `ACTIVE`.
8.  Mark the application as `APPROVED`.
9.  Store review timestamps.

The initial administrator should be explicitly confirmed rather than
inferred from the applicant.

## Step 9: Employee Invitation

The initial administrator or another authorized administrator invites an
employee.

The invitation contains:

-   Email address
-   Airline
-   Proposed role
-   Expiration timestamp
-   Secure invitation token

## Step 10: Invitation Acceptance

The invitee:

1.  Opens the invitation.
2.  Registers or logs in.
3.  Verifies that the invitation email matches the account email.
4.  Accepts the invitation.
5.  The system validates the token and expiration.
6.  The system creates or activates the membership.
7.  The invitation is marked as accepted.

------------------------------------------------------------------------

# 14. Airline Context and Authorization

## Core Principle

A user logs in to the GDS as a user. They do not need a separate login
account for every airline.

After login, the user operates within an airline context.

A user may have:

-   One role in Airline A
-   A different role in Airline B
-   No access to Airline C

## Example

``` text
John -> ABC Airways -> OWNER
John -> XYZ Airways -> VIEWER
Mary -> ABC Airways -> ADMIN
```

## Example Request

``` http
GET /api/v1/airlines/{airlineId}/members
Authorization: Bearer <jwt>
```

The system should:

1.  Extract the user ID from the JWT.
2.  Extract the airline ID from the route.
3.  Find the user's membership for that airline.
4.  Confirm the membership is active.
5.  Resolve the membership's role.
6.  Resolve the role's permissions.
7.  Check whether the user has the required permission.
8.  Allow or deny the request.

## Important Security Rule

Do not trust an arbitrary client-supplied header such as:

``` http
X-Airline-Id: some-airline-id
```

The route parameter should be checked against the authenticated user's
membership. A user must not be able to switch to an unauthorized airline
simply by changing a header or URL.

------------------------------------------------------------------------

# 15. Document Storage Design

## Recommended Approach

Use:

-   PostgreSQL for document metadata
-   Cloudflare R2 for the actual files

## Why Not Store Files Directly in PostgreSQL?

Object storage is generally better for uploaded documents because it
provides:

-   Better handling of large files
-   Independent storage scaling
-   Easier file delivery
-   Object-level access control
-   Better integration with signed URLs
-   Reduced database size
-   Easier storage migration

## Suggested Object Key

``` text
onboarding/{applicationId}/documents/{documentId}/certificate-of-incorporation.pdf
```

Do not use the original filename as the only object key because it may
contain unsafe characters or collide with another file.

## Storage Configuration

``` yaml
storage:
  provider: r2
  bucket: ${R2_BUCKET}
  endpoint: ${R2_ENDPOINT}
  access-key: ${R2_ACCESS_KEY}
  secret-key: ${R2_SECRET_KEY}
```

Store credentials in environment variables or a secrets manager. Never
commit them to Git.

## Storage Abstraction

``` java
public interface ObjectStorageService {

    String upload(
        String objectKey,
        InputStream inputStream,
        String contentType,
        long contentLength
    );

    InputStream download(String objectKey);

    String generateDownloadUrl(
        String objectKey,
        Duration expiration
    );

    void delete(String objectKey);
}
```

An implementation such as `CloudflareR2StorageService` can use the
S3-compatible API.

This abstraction makes it easier to replace R2 with:

-   Amazon S3
-   MinIO
-   Supabase Storage
-   Another compatible provider

## Download Strategy

Two possible approaches:

### Backend Streaming

The backend validates authorization and streams the file to the client.

Advantages:

-   Centralized access control
-   No direct storage URL exposed

Disadvantages:

-   Backend handles the file traffic
-   More server bandwidth usage

### Signed URL

The backend validates authorization and generates a short-lived signed
URL.

Advantages:

-   Efficient file delivery
-   Less backend bandwidth usage
-   Easy integration with object storage

Disadvantages:

-   URL must be short-lived
-   Access must be checked before issuing it

A short-lived signed URL is generally a good option for document
downloads.

## Storage Security

-   Keep the bucket private.
-   Do not expose permanent public URLs.
-   Validate authorization before generating download URLs.
-   Validate MIME types and file signatures.
-   Enforce file size limits.
-   Consider malware scanning.
-   Avoid path traversal vulnerabilities.
-   Do not trust the original filename.
-   Log document access where compliance requires it.

------------------------------------------------------------------------

# 16. Database Constraints and Indexes

## User Service

Recommended constraints and indexes:

-   Unique normalized user email
-   Unique role name
-   Unique permission name
-   Unique `(role_id, permission_id)`
-   Index token records by `user_id`
-   Index token records by `expires_at`
-   Index refresh tokens by `token_hash`
-   Index refresh tokens by `user_id`

**As implemented**: all of the above are in place (V1–V6). `token_hash` is
`UNIQUE` on both token tables (stronger than a plain index — matches the
"used as the lookup key" access pattern). Not in this doc's original list,
but added: `users.status`/`locked_until` are plain columns (V7, no index —
fine at current scale, `findByEmail` remains the only login-path query).

## Airline Service

Recommended constraints and indexes:

-   Unique non-null IATA code
-   Unique non-null ICAO code
-   Index applications by applicant
-   Index applications by status
-   Index documents by application
-   Index reviews by application
-   Unique `(user_id, airline_id)` for memberships
-   Index memberships by `user_id`
-   Index memberships by `airline_id`
-   Index invitations by airline
-   Index invitations by email
-   Index invitations by status
-   Index invitations by expiration
-   Index applications by registration number where appropriate

For nullable IATA and ICAO codes, use partial unique indexes if
supported by PostgreSQL.

------------------------------------------------------------------------

# 17. Transaction Boundaries

## User Registration

One local transaction should create:

-   User
-   Email verification token

Email delivery should preferably be handled through an outbox or
reliable asynchronous process.

## Airline Approval

One local Airline Service transaction should:

-   Update onboarding application
-   Create airline
-   Create initial membership
-   Record approval state

## Avoid Distributed Transactions

Do not attempt to use a distributed database transaction across User
Service and Airline Service.

Instead:

-   Keep each service's transaction local.
-   Use REST calls for necessary validation.
-   Use events later for asynchronous synchronization.
-   Design operations to be idempotent.

------------------------------------------------------------------------

# 18. Cross-Service References

The Airline Service may store:

``` text
user_id
role_id
```

These values refer logically to entities in the User Service.

They are not database foreign keys because the services use separate
databases.

## Validation Options

Initially:

-   Validate that the user exists through a User Service API when
    necessary.
-   Validate that the role exists through a controlled internal API or
    seeded configuration.
-   Cache stable role metadata where useful.

Later:

-   Publish user and role events.
-   Maintain read models.
-   Add internal service-to-service authentication.
-   Use service discovery or an API gateway.

------------------------------------------------------------------------

# 19. Error Handling

Use a consistent error response format.

Example:

``` json
{
  "timestamp": "2026-09-16T10:00:00Z",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "The request contains invalid fields",
  "path": "/api/v1/onboarding/applications"
}
```

## Suggested Error Codes

``` text
VALIDATION_ERROR
RESOURCE_NOT_FOUND
DUPLICATE_RESOURCE
INVALID_CREDENTIALS
TOKEN_EXPIRED
TOKEN_INVALID
ACCESS_DENIED
INVALID_APPLICATION_STATUS
DOCUMENT_UPLOAD_FAILED
STORAGE_ERROR
CONFLICT
INTERNAL_ERROR
```

Do not expose:

-   Stack traces
-   Passwords
-   Tokens
-   Internal database details
-   Sensitive provider errors

### As implemented

The shared `ErrorResponse` (`common-lib`, used repo-wide, not user-service-
specific) is `{timestamp, status, error, message, path}` — **no `code`
field**, so the suggested `VALIDATION_ERROR`/`DUPLICATE_RESOURCE`/etc.
machine-readable codes above were never added. Status codes in practice:
`400` for validation and most auth/credential failures (including the
"don't expose" list above — confirmed no stack traces or raw exception
messages leak, a catch-all handler always substitutes a generic message for
anything unhandled), `403` for duplicate-role/duplicate-permission creation
(`OperationNotPermittedException`), `404` for not-found lookups. Adding a
`code` field would be a `common-lib` change (affects every service that
depends on it), not a `user-service`-local one.

------------------------------------------------------------------------

# 20. Security Requirements

## Authentication

-   Hash passwords with BCrypt or Argon2.
-   Use short-lived access tokens.
-   Use refresh token rotation.
-   Support refresh token revocation.
-   Verify email addresses.
-   Rate-limit login attempts.
-   Rate-limit verification email requests.

### As implemented (User Service)

BCrypt ✓. Short-lived (15 min) access tokens ✓. Refresh rotation ✓ (every
`refresh()` call revokes the presented token and issues a new one).
Revocation ✓ (explicit `/auth/logout`, plus automatic mass-revocation on
detected refresh-token reuse). Email verification ✓, and — beyond this
doc — actually enforced as a login gate (section 1a). **"Rate-limit login
attempts" is implemented as per-account lockout** (`failed_login_attempts`/
`locked_until`, default: lock for 15 min after 5 consecutive failures),
**not IP-based rate limiting** — there's no throttle on distinct accounts
being tried from one source. **"Rate-limit verification email requests" is
not implemented at all** — `/auth/resend-verification` can be called
without limit for any email.

## Authorization

-   Apply airline-scoped authorization.
-   Check membership status.
-   Check role permissions.
-   Prevent insecure direct object references.
-   Restrict onboarding review to authorized GDS administrators.
-   Restrict airline membership management to authorized airline roles.

## Document Security

-   Use private object storage.
-   Validate file types and signatures.
-   Enforce file size limits.
-   Use signed URLs with short expiration.
-   Validate access before issuing URLs.
-   Consider malware scanning.
-   Record access events where necessary.

## Operational Security

-   Use HTTPS.
-   Store secrets outside source control.
-   Do not log passwords or tokens.
-   Use structured logs.
-   Apply rate limiting.
-   Monitor authentication failures.
-   Add audit logging for sensitive operations.

------------------------------------------------------------------------

# 21. Suggested Audit Log Design

Audit logging can be introduced in this phase or shortly afterward.

A future `audit_logs` table may contain:

  Field             Description
  ----------------- -----------------------------------
  `id`              Audit record ID
  `user_id`         User who performed the action
  `airline_id`      Related airline, if applicable
  `action`          Action performed
  `resource_type`   Type of resource
  `resource_id`     Related resource
  `timestamp`       Event time
  `ip_address`      Request IP
  `metadata`        Additional structured information

Examples of auditable actions:

``` text
SUBMIT_ONBOARDING_APPLICATION
UPLOAD_ONBOARDING_DOCUMENT
VERIFY_DOCUMENT
APPROVE_AIRLINE
REJECT_AIRLINE
INVITE_MEMBER
ACCEPT_INVITATION
CHANGE_MEMBER_ROLE
SUSPEND_MEMBER
REMOVE_MEMBER
DOWNLOAD_DOCUMENT
```

Audit records should be append-only and should not contain secrets.

------------------------------------------------------------------------

# 22. Suggested Project Structure

## User Service

``` text
src/main/java/com/example/userservice
├── config
├── controller
├── dto
├── entity
├── enums
├── exception
├── mapper
├── repository
├── security
└── service
```

## Airline Service

``` text
src/main/java/com/example/airlineservice
├── config
├── controller
├── dto
├── entity
├── enums
├── exception
├── mapper
├── repository
├── security
├── service
└── storage
```

The exact package layout can evolve, but separating controllers,
services, persistence, security, and storage keeps responsibilities
clear.

------------------------------------------------------------------------

# 23. Flyway Migration Plan

## User Service

``` text
V1__create_users.sql
V2__create_roles.sql
V3__create_permissions.sql
V4__create_role_permissions.sql
V5__create_email_verification_tokens.sql
V6__create_refresh_tokens.sql
V7__seed_roles.sql
V8__seed_permissions.sql
V9__seed_role_permissions.sql
```

**As implemented**: `V1`–`V6` exist exactly as named above. `V7` is instead
`V7__add_user_status_and_lockout.sql` (adds `status`/`failed_login_attempts`/
`locked_until` to `users` — not in this doc's original plan). **`V7`–`V9`
seed migrations (as originally numbered) were never written** — `roles`,
`permissions`, and `role_permissions` all start empty; see section 6.2's
implementation note. Every migration uses `CREATE TABLE IF NOT EXISTS`
(idempotent — safe to re-run against a partially-migrated dev DB), which
isn't a Flyway requirement but was adopted as a defensive habit here.

## Airline Service

``` text
V1__create_airlines.sql
V2__create_onboarding_applications.sql
V3__create_onboarding_documents.sql
V4__create_onboarding_reviews.sql
V5__create_airline_memberships.sql
V6__create_airline_invitations.sql
```

Migration files should define:

-   Columns
-   Constraints
-   Indexes
-   Enum strategy
-   Default timestamps where appropriate

------------------------------------------------------------------------

# 24. Testing Strategy

## User Service Unit Tests

Test:

-   Registration validation
-   Duplicate email handling
-   Password hashing
-   Email verification
-   Expired verification tokens
-   Reused verification tokens
-   Login with invalid credentials
-   Disabled or suspended accounts
-   Access token generation
-   Refresh token rotation
-   Logout and token revocation
-   Role-permission resolution

## Airline Service Unit Tests

Test:

-   Draft creation
-   Draft updates
-   Invalid application status transitions
-   Required document validation
-   File size validation
-   File type validation
-   Document rejection
-   Review decisions
-   Approval rules
-   Initial administrator selection
-   OWNER membership creation
-   Invitation creation
-   Invitation expiration
-   Invitation acceptance
-   Duplicate memberships
-   Airline-scoped authorization

## Integration Tests

Use Testcontainers or equivalent infrastructure to test:

-   PostgreSQL schema
-   Flyway migrations
-   Multipart uploads
-   Storage integration
-   Signed URL generation
-   Transaction rollback
-   API authorization
-   Cross-service validation

------------------------------------------------------------------------

# 25. Development Milestones

## Milestone 1: User Registration

Implement:

-   User entity
-   Registration DTOs
-   Password hashing
-   User repository
-   Registration service
-   Email verification token

## Milestone 2: Authentication

Implement:

-   Login
-   JWT access token
-   Refresh token
-   Logout
-   Token revocation
-   Spring Security configuration

## Milestone 3: Roles and Permissions

Implement:

-   Role entity
-   Permission entity
-   Role-permission mapping
-   Seed migrations
-   Permission resolution

## Milestone 4: Airline Onboarding

Implement:

-   Onboarding application entity
-   Draft creation
-   Draft updates
-   Submission validation
-   Status transitions

## Milestone 5: Document Storage

Implement:

-   Document metadata
-   R2 storage abstraction
-   Upload validation
-   Private bucket
-   Signed download URLs

## Milestone 6: Review

Implement:

-   Reviewer endpoints
-   Review history
-   Approval
-   Rejection
-   Requested changes

## Milestone 7: Airline Creation and OWNER Membership

Implement:

-   Airline creation during approval
-   Explicit initial administrator selection
-   OWNER membership creation
-   Transactional approval process

## Milestone 8: Invitations

Implement:

-   Invitation creation
-   Invitation token
-   Invitation acceptance
-   Membership activation
-   Role assignment

## Milestone 9: Airline Context Authorization

Implement:

-   Airline-scoped routes
-   Membership lookup
-   Role and permission checks
-   Access denial handling
-   IDOR protection

------------------------------------------------------------------------

# 26. Complete Acceptance Scenario

The following scenario should work end-to-end:

1.  John registers a GDS account.
2.  John verifies his email.
3.  John logs in.
4.  John creates an onboarding draft for ABC Airways.
5.  John uploads the required company documents.
6.  John submits the application.
7.  A GDS administrator reviews the application.
8.  The administrator confirms the initial administrator explicitly.
9.  The application is approved.
10. The system creates ABC Airways.
11. The system creates an active membership for the selected initial
    administrator.
12. The selected administrator receives the `OWNER` role.
13. John can view the airlines he belongs to.
14. John invites Mary to ABC Airways.
15. Mary accepts the invitation.
16. Mary becomes an `ADMIN` member of ABC Airways.
17. John can belong to another airline with a different role.
18. Requests to ABC Airways are authorized using John's ABC membership.
19. Requests to XYZ Airways are authorized using John's XYZ membership.
20. A user without an active membership is denied access.

------------------------------------------------------------------------

# 27. Recommended Implementation Order

## User Service — all items below are implemented and manually end-to-end tested

1.  User entity — ✅ (`Long` id, not UUID; extra `status`/lockout fields)
2.  Registration — ✅ (`POST /auth/signup`; issues no tokens, see section 1a)
3.  Password hashing — ✅ (BCrypt)
4.  Email verification — ✅ (enforced as a login gate; `GET`+`POST /auth/verify-email`)
5.  Login — ✅ (`POST /auth/login`; includes account-lockout checks)
6.  JWT security — ✅ (HMAC-signed, secret externalized with no fallback — see `CLAUDE.md`)
7.  Refresh tokens — ✅ (rotation + reuse detection, see section 6.6)
8.  Logout and revocation — ✅ (`POST /auth/logout`)
9.  Roles — ✅ (full CRUD, unseeded, unconsumed — see section 6.2)
10. Permissions — ✅ (full CRUD, unseeded, unconsumed — see section 6.3)
11. Role-permission mappings — ✅ (assign/unassign API, see section 6.4)

## Airline Service — update (2026-09-20)

1.  Onboarding application — ✅ (`AirlineOnboardingApplication`, `/api/onboarding/applications`, Bean-Validated on create — section 10.2)
2.  Application status transitions — ✅ (`DRAFT`→`SUBMITTED`→`APPROVED`/`REJECTED`, `REQUESTED_CHANGES` loops back to `DRAFT`)
3.  Document metadata — not started
4.  Object storage integration — not started
5.  Document upload validation — not started
6.  Review workflow — ✅ (`OnboardingReviewController`, append-only `OnboardingReview`, `ReviewDecision` enum — section 10.4)
7.  Airline creation — ✅ (only via onboarding approval; no self-serve `POST /api/airlines` anymore)
8.  Initial administrator selection — ✅ (`initialAdminUserId`, required before approval)
9.  OWNER membership — ✅ (`AirlineMembership`, `roleId` from the `airline.owner-role-id` config value — section 10.5)
10. Invitations — not started
11. Invitation acceptance — not started
12. Airline-scoped authorization — partial: `requireActiveMembership` checks membership *existence* per airline, not yet role/permission-granular (no `@PreAuthorize`/role check anywhere yet — see `CLAUDE.md`)

No authorization is enforced yet on the admin review side either (`OnboardingReviewController`) — same deferred posture as items above and as `user-service`'s `/api/users`, `/api/roles`.

**Airline lifecycle (controller review, 2026-09-20):** an airline is closed by *soft delete* (`status = INACTIVE`, memberships and aircraft kept — a hard delete can't work since memberships FK-reference the airline). Only `ACTIVE` airlines can be edited or closed by members; platform-side status changes go through `/activate`, `/suspend`, `/ban` (no no-op transitions, and an `INACTIVE` airline's status is locked). IATA/ICAO uniqueness clashes on update return `409`.

## First Complete Target

The first complete business capability should be:

``` text
Register
  -> Verify Email
  -> Login
  -> Create Application
  -> Upload Documents
  -> Submit Application
  -> Review
  -> Approve
  -> Create Airline
  -> Create OWNER Membership
  -> Invite Employee
  -> Accept Invitation
  -> Create Employee Membership
  -> Enforce Airline-Specific Permissions
```

------------------------------------------------------------------------

# 28. Final Entity Checklist

**Note (2026-09-18): this checklist was written aspirationally, before any
code existed. It's kept as-is below for history, but read it against
section 1a and the per-entity/per-section "As implemented" notes — several
`[x]` items here (`Rate limiting`, `Airline-scoped authorization`, `Private
document storage`) describe the *design intent*, not something actually
built. Concretely: User Service entities/JWT/refresh-revocation/email-
verification/password-hashing are genuinely done; role/permission checks
exist only as unconsumed CRUD; rate limiting is per-account lockout, not
true rate limiting; audit logging is still just a plan (section 21, no
`audit_logs` table exists); everything Airline-Service-related (private
document storage, airline-scoped authorization) hasn't been started.**

## User Service

-   [x] User
-   [x] Role
-   [x] Permission
-   [x] RolePermission
-   [x] EmailVerificationToken
-   [x] RefreshToken

## Airline Service

-   [x] Airline
-   [x] AirlineOnboardingApplication
-   [x] OnboardingDocument
-   [x] OnboardingReview
-   [x] AirlineMembership
-   [x] AirlineInvitation

## Storage

-   [x] Private object storage
-   [x] Storage abstraction
-   [x] Upload validation
-   [x] File metadata
-   [x] Signed download URLs
-   [x] Authorization checks
-   [x] File size limits
-   [x] Error handling

## Security

-   [x] Password hashing
-   [x] JWT authentication
-   [x] Refresh token revocation
-   [x] Email verification
-   [x] Airline-scoped authorization
-   [x] Role and permission checks
-   [x] Private document storage
-   [x] Audit logging plan
-   [x] Rate limiting

------------------------------------------------------------------------

# 29. Core Design Decisions Summary

1.  `User` and `Airline` have a many-to-many relationship.
2.  The relationship is represented by `AirlineMembership`.
3.  `userId` should not be stored directly on the `Airline` entity.
4.  Roles are assigned through airline memberships.
5.  Roles and permissions are owned by the User Service.
6.  Memberships are owned by the Airline Service.
7.  Cross-service IDs are logical references, not database foreign keys.
8.  The applicant is not automatically the airline owner.
9.  The initial administrator must be explicitly selected or confirmed.
10. `OWNER` means the highest GDS administrative role within the airline
    organization.
11. Users log in once to the GDS and then operate in an airline context.
12. Airline-scoped routes should use the airline ID in the URL and
    verify membership using the authenticated user ID.
13. IATA and ICAO codes are externally assigned and should not be
    generated by the GDS.
14. PostgreSQL stores document metadata, while object storage stores the
    actual files.
15. Documents should be stored in a private bucket and accessed through
    authorization-protected signed URLs.
16. Local transactions should be used within each service; distributed
    transactions should be avoided.
17. Review history should be preserved rather than overwritten.
18. The initial scope should focus on identity, onboarding,
    organization, membership, and authorization before implementing
    booking and ticketing functionality.
