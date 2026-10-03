# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Civitas is a multi-tenant SaaS platform for managing associations and unions ("verenigingen").
Core focus: strict data isolation between tenants ("unions") at every layer of the application.

## Commands

```bash
# Run the application (http://localhost:8080)
./mvnw spring-boot:run

# Build (skip tests)
./mvnw clean package -DskipTests

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=ClassName

# Run a single test method
./mvnw test -Dtest=ClassName#methodName

# Compile only (run after any change)
./mvnw compile
```

## Tech Stack

- **Spring Boot 3.5.5**, Java 21, Maven
- **Spring Data JPA** (Hibernate) + MySQL 8
- **Spring Security** — form-based login, BCrypt passwords
- **Thymeleaf** — server-side rendering, i18n (EN/NL/TR via `i18n/messages*.properties`)
- **Lombok** — used on all domain/DTO classes
- **Stripe** — payment links + webhook handling
- **Twilio** — WhatsApp notifications
- **OpenHTMLToPDF** — PDF generation from Thymeleaf templates
- **Testcontainers** — integration tests spin up real MySQL

## Architecture

### Package Structure

```
controller/          — Page controllers (Thymeleaf) + REST controllers (JSON/AJAX)
service/             — Business logic
  ├── kpi/           — Dashboard KPI providers (one class per metric)
  ├── accounting/    — PDF generation, invoice scanning, file storage
  └── communication/ — Notifications, WhatsApp
domain/              — JPA entities
repository/          — Spring Data JPA interfaces
listener/            — @TransactionalEventListener handlers
dto/                 — Request/response objects
util/                — InitDataConfig (seed data), EventReminderTask (scheduled)
validator/           — Custom Bean Validation (e.g. MemberEmailValidator)
config/              — SecurityConfig
```

### Multi-Tenancy Pattern (read this before touching any service or repository)

Every tenant-scoped entity has a `@ManyToOne Union union` field. **All data access is manually scoped in the service layer** — there is no global filter, so this must be applied by hand, every time.

The pattern used in every service:

```java
private Union getCurrentUserUnion() {
    Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    if (principal instanceof MyUser) return ((MyUser) principal).getUnion();
    throw new RuntimeException("No user logged in");
}
```

Rules that must hold for every service method, with no exceptions:
1. **Read**: query with `findAllByUnion(getCurrentUserUnion(), ...)` — never query without union scope.
2. **Create**: stamp new entity with `entity.setUnion(createdByUser.getUnion())` before saving.
3. **Update/Delete**: call `verifyXxxBelongsToUnion(entity)` before mutating. Treat `AccessDeniedException` as "not found" to avoid information leakage (see `MemberServiceImpl.findById`).

**Adding a new tenant-scoped entity — follow this checklist every time:**
1. Add `@ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "union_id", nullable = false) private Union union;`
2. Repository methods must all accept `Union union` as a filter parameter.
3. Service create method: `entity.setUnion(getCurrentUserUnion())`.
4. Service read/update/delete: call a `verifyXxxBelongsToUnion()` guard before any mutation.

### Event-Driven Notifications

Services publish domain events via `ApplicationEventPublisher` (e.g., `MemberSavedEventDto`, `TransactionCreatedDto`). Listeners in `listener/` annotated with `@TransactionalEventListener` consume these to create `Notification` records. This keeps notification logic decoupled from the committing transaction.

### Dashboard KPIs

Each dashboard tile is a `KpiProvider` implementation (in `service/kpi/`). `DashboardServiceImpl` collects them all and returns `KpiTileDto` lists. Add a new tile by implementing `KpiProvider` and registering it as a Spring bean — all queries must be union-scoped.

### Database

**Flyway owns the schema.** `spring.jpa.hibernate.ddl-auto=validate` — Hibernate never changes the schema, it only fails startup if the entities don't match it. `InitDataConfig` (`CommandLineRunner`) seeds two test unions and demo data on each start.

- Migrations live in `src/main/resources/db/migration/`, named `V{n}__{snake_case_description}.sql` (next number in sequence, double underscore). `V1__baseline.sql` is the schema as it was when Flyway was introduced; existing DBs were baselined at version 1, so it only runs on empty DBs.
- Any entity change that affects the schema (new field/entity, rename, type/length change, **new enum constant** — `@Enumerated(STRING)` maps to a native MySQL `ENUM`, and `validate` doesn't compare its value list) needs a new migration in the same commit.
- Never edit a migration that has already been applied (checksum mismatch fails startup); write a new version instead. MySQL DDL isn't transactional, so keep migrations small.
- `docs/migrations/` holds pre-Flyway, manually applied scripts — history only, already part of V1.
- A non-empty DB without `flyway_schema_history` fails startup on purpose (`baseline-on-migrate=false`). To adopt an existing DB, start once with `SPRING_FLYWAY_BASELINE_ON_MIGRATE=true`, then remove it.
- Testcontainers integration tests (`MySqlIntegrationTest` subclasses) use `create-drop` with Flyway disabled; `FlywayBaselineIntegrationTest` is the one test that runs the real migrations + `validate`.

**Local dev credentials** (in `application.properties`):
- DB: `jdbc:mysql://localhost:3306/civitas_db`, user `civitas_user` / `root`
- Login: `apo` / `apo` (Civitas Demo Union), `admin_gent` / `gent123` (Student Union Ghent)

### Security

- All routes require authentication except `/`, `/login**`, `/css/**`, `/js/**`, `/error`, `/stripe/webhook`
- CSRF enabled everywhere except `/stripe/webhook`
- No `@PreAuthorize` annotations — union authorization is done manually in service methods (see Multi-Tenancy Pattern above)

## Development Rules

- **Language:** Code, comments, and this file are in English. User-facing UI text and `messages*.properties` are NL/EN/TR.
- **Testing:** Write JUnit 5/Mockito tests for every bugfix or new feature.
- **Workflow:** Show a plan before modifying files. Run `./mvnw compile` after changes.

## Active Work

The current task list lives in `TODO.md`, not here — check it separately when picking up work; it is not reloaded automatically with this file.