# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo is

A Spring Boot multi-module microservices GDS (Global Distribution System) for an airline platform: flight search, seat selection, booking, payment, and notifications, behind an API gateway with JWT auth, Eureka service discovery, a Git-backed Config Server, and Kafka for async events. See `README.md` for the full original feature/architecture writeup (services table, data-flow diagrams, security model, API examples, circuit-breaker thresholds) — it's accurate for the platform as a whole, with one correction: the actual Java package root is `com.sunday` (e.g. `com.sunday.common_lib`, `com.sunday.services`).

**`user-service` is currently mid-redesign** against `airline-gds-complete-user-airline-design.md` (an airline-only GDS identity/auth design doc). It has diverged from the rest of the repo in ways described below — read that section before touching `services/user-service`.

## Commands

Build everything:
```bash
mvn clean package -DskipTests          # from repo root
```

Build/compile just what you're touching (much faster than a full reactor build):
```bash
mvn -pl common-lib,services/user-service -am compile -q
```
`-am` ("also make") pulls in upstream reactor dependencies (here, `common-lib`). This works fine for `compile`/`install`/`test` goals.

Run a single service locally — **do not** use `-pl <module> -am spring-boot:run` from the repo root. Maven runs the `spring-boot:run` goal against every project in the resulting reactor, including the root and `services` aggregator POMs (packaging `pom`, no main class), and fails immediately with `Unable to find a suitable main class`. Instead:
```bash
mvn -pl common-lib install -DskipTests   # once, so it's resolvable from the local repo
cd services/user-service                # or whichever service
SPRING_PROFILES_ACTIVE=local mvn spring-boot:run
```
(On Windows PowerShell: `$env:SPRING_PROFILES_ACTIVE = 'local'` first, or set it in an `application-local.yaml`-adjacent way — the `local` profile is what supplies concrete `localhost` values for the datasource and Config Server URL; the base `application.yaml` uses unresolved `${...}` env-var placeholders that assume a container/cluster environment.)

Local infra a service needs before it'll boot cleanly: MySQL (`localhost:3306`, one DB per service, e.g. `airline_user`), Config Server (`localhost:8888`), Eureka (`localhost:8761`). `spring.config.import` is `optional:configserver:...`, so a service will still start without the Config Server, just without whatever config it serves (locations, Flyway/JPA settings for `user-service` — see below).

Run tests:
```bash
mvn -pl services/user-service test
```
Every service currently only has a boilerplate `*ApplicationTests.contextLoads()` smoke test — no real test coverage exists yet anywhere in the repo.

Full stack via Docker Compose: `docker compose -f docker-compose/docker-compose.yml up -d` (see README's "Running the Project" section for env vars required — Gmail SMTP, Twilio, Razorpay/Stripe, `JWT_SECRET_KEY`).

## Architecture (condensed — see README.md for the full picture)

- **Module layout**: `common-lib` (shared DTOs/enums/exceptions/events, package `com.sunday.common_lib`) → `cloud/` (api-gateway, config-server, service-registry) → `services/*` (11 business services, each its own Spring Boot app + its own MySQL DB, package `com.sunday.services`).
- **Auth model**: JWT is validated once at `api-gateway`; the gateway forwards `X-User-Id` / `X-User-Email` / `X-User-Roles` headers downstream. Business services **trust these headers** rather than re-validating the JWT themselves — this is a repo-wide convention, not specific to one service.
- **Inter-service calls**: OpenFeign clients, each with a `*ClientFallback.java` and a Resilience4j circuit breaker (per-client thresholds in README).
- **Async events**: Kafka, e.g. `booking.confirmed` / `payment.completed`, consumed by `seat-service` and `notification-service` in parallel (saga-by-choreography, no distributed transactions).
- **IDs are `Long`** everywhere (not UUID) — an explicit, repo-wide decision; don't introduce UUID PKs in one service without updating the gateway and every Feign consumer of that service.

## `user-service` redesign — read before editing this service

`services/user-service` is being rebuilt incrementally against `airline-gds-complete-user-airline-design.md`, and has already diverged from both that doc and the rest of the repo in ways worth knowing up front:

- **`User.role` was removed entirely.** The old global `UserRole` enum (`ROLE_SYSTEM_ADMIN` / `ROLE_AIRLINE_OWNER` / `ROLE_CUSTOMER`, still defined in `common-lib`) is no longer stored on `User` at all. Practical effect: `CustomUserDetailsService` now always grants **empty authorities**, so any user created through the current signup flow has no role, and the JWT `authorities` claim is always empty. This means gateway/service role-based authorization (as described in the README's "Security" section) **does not currently work** for anything going through the redesigned signup — this is a known, intentional-for-now gap, not a bug to silently "fix" by re-adding a role field unless asked.
- **A separate `Role`/`Permission`/`RolePermission` model exists** (own tables, own CRUD under `/api/roles`, `/api/permissions`) — deliberately **not** wired to the old platform `UserRole` enum or to the User entity. `Role` now has a `scope` (`PLATFORM` or `AIRLINE`, default `AIRLINE`, migration `V8`): `AIRLINE`-scoped roles (`OWNER`/`ADMIN`/`VIEWER` — `OPERATIONS_MANAGER`/`BOOKING_AGENT` deliberately not created yet, since flight-ops/booking don't exist in this phase) are meant to be granted via `AirlineMembership`, which doesn't exist yet (`airline-core-service`'s future work) — still unconsumed. `PLATFORM`-scoped roles (`GDS_ADMIN`, for reviewing onboarding applications) have their own direct grant mechanism instead, since there's no membership to hang them off: `UserPlatformRole` (migration `V9`), assigned via `POST/DELETE /api/roles/{roleId}/users/{userId}` and read via `GET /api/users/{userId}/roles`. Assigning an `AIRLINE`-scoped role through that endpoint is rejected (`403`) — it's `PLATFORM`-only. No Flyway seed data for any of this (consistent with the rest of `roles`/`permissions`) — a real 4-role/10-permission set (`GDS_ADMIN`, `OWNER`, `ADMIN`, `VIEWER`) has been created and wired through the API in the live dev DB as part of verifying this, though — not throwaway test junk, don't delete it without checking.
- **`User` fields changed**: `fullName` → `firstName`/`lastName`/`middleName`; `phone` → `phoneNumber`; `verified` → `emailVerified`. `common-lib`'s `UserDTO` was updated to match, which is a **breaking change** for `booking-service`'s and `payment-service`'s `UserClient` Feign clients (they still expect the old shape/path) — updating those was explicitly deferred, not forgotten.
- **Flyway was added to `user-service` only** (`services/user-service/src/main/resources/db/migration/`) — other services still rely on implicit Hibernate schema generation. **Spring Boot 4 gotcha**: `flyway-core`/`flyway-mysql` on the classpath is *not* enough to trigger migrations — `FlywayAutoConfiguration` was split into its own artifact, `org.springframework.boot:spring-boot-flyway`, which must be an explicit dependency too (see `services/user-service/pom.xml`). Without it, Flyway silently never runs (no `flyway_schema_history` table, no error) while Hibernate's `ddl-auto: validate` then fails with a confusing "missing table" error — this cost real debugging time once already.
- **`spring.jpa.hibernate.ddl-auto` and `spring.flyway.*` for `user-service` live in the Config Server's repo**, not in the local `application.yaml`/`application-local.yaml` — check there (or query `http://localhost:8888/user-service/local` while the Config Server is up) before assuming schema behavior from the local YAML alone.
- **Auth response shape**: `AuthResponse.user` is `AuthUserDTO` (no `password`/`username` fields at all, structurally), not the shared `UserDTO` — `UserController` no longer references `UserDTO` at all; it's kept only because `booking-service`'s and `payment-service`'s `UserClient` Feign clients still declare it as their return type (the currently-stale-shaped downstream consumers).
- **JWT access tokens are short-lived (15 min)**; refresh tokens are opaque random strings (SHA-256-hashed at rest, never stored raw), support rotation, and detect reuse by revoking every active token for a user if an already-rotated token is presented again — that revocation is deliberately `noRollbackFor = UserException.class` on `refresh()`, because the rejection thrown right after it would otherwise roll the revocation back and silently disable the only stolen-refresh-token defense. **The JWT secret has no fallback anywhere** — `jwt.secret` is `${JWT_SECRET_KEY}` with no default in both `user-service` and `api-gateway`'s base `application.yaml`; both must be given the *same* value (env var / Config Server / `.env` for docker-compose) or every token either service issues gets rejected by the other. Local dev values live in each module's `application-local.yaml`.
- **Email verification and account lockout are now enforced** (not decorative): `signup` no longer issues any tokens — just a "check your email" response; `login`/`refresh` reject an unverified account (`EMAIL_NOT_VERIFIED`), a locked account (`ACCOUNT_LOCKED`, after `auth.max-failed-login-attempts` consecutive bad passwords, for `auth.account-lockout-minutes`), or a non-`ACTIVE` `User.status`. Failed-login tracking and the account-locked/not-active checks happen before password verification, so (by design, like most systems) a locked account reveals its lock state to an unauthenticated caller — that's an accepted tradeoff, not an oversight.
- **`DataInitializationComponent`'s seeded admin account only runs under the `local` Spring profile** (`@Profile("local")`) and reads its password from `bootstrap.admin.password` (only defined in `application-local.yaml`) — it must never run, and never gets a config value to run with, outside local dev.
- **`GET /api/users` and `GET /api/users/{userId}` have no authorization check at all** — any authenticated caller (i.e. anyone who can self-register) can currently list or enumerate every user in the system. This is a known, deferred gap — the user plans to close it from `api-gateway` as a single RBAC entry point once roles/permissions are wired through, not inside `user-service` itself. Don't "fix" this locally without checking that plan first.
- **Multi-write service methods are `@Transactional`** (`AuthServiceImpl.signup/login/verifyEmail/resendVerification/refresh`, `RoleServiceImpl.assignPermissionsToRole`, `TokenCleanupScheduler.purgeExpiredTokens`) — keep that pattern for any new method that writes more than one row/table in one logical operation; Spring Data's `save()` is otherwise transactional per-call, not per-business-operation. Watch for the `refresh()`-style exception: if a throw *after* a write is meant to reject the request while still keeping that write, you need `noRollbackFor`, not a bare `@Transactional`.
- **A daily scheduled job (`TokenCleanupScheduler`, 3am by default, `auth.token-cleanup-cron`) purges expired rows from `refresh_tokens` and `email_verification_tokens`** — requires `@EnableScheduling` on `UserServiceApplication`, already added.
