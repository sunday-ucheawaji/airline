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

Full stack via Docker Compose: `docker compose -f docker-compose/docker-compose.yml up -d` (see README's "Running the Project" section for env vars required — Gmail SMTP, Twilio, Razorpay/Stripe, `JWT_SECRET`).

## Architecture (condensed — see README.md for the full picture)

- **Module layout**: `common-lib` (shared DTOs/enums/exceptions/events, package `com.sunday.common_lib`) → `cloud/` (api-gateway, config-server, service-registry) → `services/*` (11 business services, each its own Spring Boot app + its own MySQL DB, package `com.sunday.services`).
- **Auth model**: JWT is validated once at `api-gateway`; the gateway forwards `X-User-Id` / `X-User-Email` / `X-User-Roles` headers downstream. Business services **trust these headers** rather than re-validating the JWT themselves — this is a repo-wide convention, not specific to one service.
- **Inter-service calls**: OpenFeign clients, each with a `*ClientFallback.java` and a Resilience4j circuit breaker (per-client thresholds in README).
- **Async events**: Kafka, e.g. `booking.confirmed` / `payment.completed`, consumed by `seat-service` and `notification-service` in parallel (saga-by-choreography, no distributed transactions).
- **IDs are `Long`** everywhere (not UUID) — an explicit, repo-wide decision; don't introduce UUID PKs in one service without updating the gateway and every Feign consumer of that service.

## `user-service` redesign — read before editing this service

`services/user-service` is being rebuilt incrementally against `airline-gds-complete-user-airline-design.md`, and has already diverged from both that doc and the rest of the repo in ways worth knowing up front:

- **`User.role` was removed entirely.** The old global `UserRole` enum (`ROLE_SYSTEM_ADMIN` / `ROLE_AIRLINE_OWNER` / `ROLE_CUSTOMER`, still defined in `common-lib`) is no longer stored on `User` at all. Practical effect: `CustomUserDetailsService` now always grants **empty authorities**, so any user created through the current signup flow has no role, and the JWT `authorities` claim is always empty. This means gateway/service role-based authorization (as described in the README's "Security" section) **does not currently work** for anything going through the redesigned signup — this is a known, intentional-for-now gap, not a bug to silently "fix" by re-adding a role field unless asked.
- **A separate `Role`/`Permission`/`RolePermission` model exists** (own tables, own CRUD under `/api/roles`, `/api/permissions`), but it's an *additive*, airline-membership-scoped concept (`OWNER`/`ADMIN`/`OPERATIONS_MANAGER`/`BOOKING_AGENT`/`VIEWER`) per the design doc — deliberately **not** wired to the old platform `UserRole` enum or to the User entity. Nothing consumes it yet (there's no `AirlineMembership` — that's `airline-core-service`'s future work).
- **`User` fields changed**: `fullName` → `firstName`/`lastName`/`middleName`; `phone` → `phoneNumber`; `verified` → `emailVerified`. `common-lib`'s `UserDTO` was updated to match, which is a **breaking change** for `booking-service`'s and `payment-service`'s `UserClient` Feign clients (they still expect the old shape/path) — updating those was explicitly deferred, not forgotten.
- **Flyway was added to `user-service` only** (`services/user-service/src/main/resources/db/migration/`) — other services still rely on implicit Hibernate schema generation. **Spring Boot 4 gotcha**: `flyway-core`/`flyway-mysql` on the classpath is *not* enough to trigger migrations — `FlywayAutoConfiguration` was split into its own artifact, `org.springframework.boot:spring-boot-flyway`, which must be an explicit dependency too (see `services/user-service/pom.xml`). Without it, Flyway silently never runs (no `flyway_schema_history` table, no error) while Hibernate's `ddl-auto: validate` then fails with a confusing "missing table" error — this cost real debugging time once already.
- **`spring.jpa.hibernate.ddl-auto` and `spring.flyway.*` for `user-service` live in the Config Server's repo**, not in the local `application.yaml`/`application-local.yaml` — check there (or query `http://localhost:8888/user-service/local` while the Config Server is up) before assuming schema behavior from the local YAML alone.
- **Auth response shape**: `AuthResponse.user` is `AuthUserDTO` (no `password`/`username` fields at all, structurally), not the shared `UserDTO` — `UserDTO` is kept only for `UserController`'s `GET` endpoints and for the (currently stale-shaped) downstream Feign consumers.
- **JWT access tokens are short-lived (15 min)**; refresh tokens are opaque random strings (SHA-256-hashed at rest, never stored raw), support rotation, and detect reuse by revoking every active token for a user if an already-rotated token is presented again. The JWT signing secret is externalized via the Config Server — it must stay in sync with whatever `api-gateway` uses to validate tokens, since that module is out of scope for this redesign and hasn't been touched.
- **Multi-write service methods are `@Transactional`** (`AuthServiceImpl.signup/login/verifyEmail/refresh`, `RoleServiceImpl.assignPermissionsToRole`) — keep that pattern for any new method that writes more than one row/table in one logical operation; Spring Data's `save()` is otherwise transactional per-call, not per-business-operation.
