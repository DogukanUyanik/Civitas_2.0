# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

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

### Multi-Tenancy Pattern

Every tenant-scoped entity has a `@ManyToOne Union union` field. **All data access is manually scoped in the service layer** — there is no global filter.

The pattern used in every service:

```java
private Union getCurrentUserUnion() {
    Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    if (principal instanceof MyUser) return ((MyUser) principal).getUnion();
    throw new RuntimeException("No user logged in");
}
```

Rules that must hold for every service method:
1. **Read**: query with `findAllByUnion(getCurrentUserUnion(), ...)` — never query without union scope.
2. **Create**: stamp new entity with `entity.setUnion(createdByUser.getUnion())` before saving.
3. **Update/Delete**: call `verifyXxxBelongsToUnion(entity)` before mutating. Treat `AccessDeniedException` as "not found" to avoid information leakage (see `MemberServiceImpl.findById`).

### Event-Driven Notifications

Services publish domain events via `ApplicationEventPublisher` (e.g., `MemberSavedEventDto`, `TransactionCreatedDto`). Listeners in `listener/` annotated with `@TransactionalEventListener` consume these to create `Notification` records. This keeps notification logic decoupled from the committing transaction.

### Dashboard KPIs

Each dashboard tile is a `KpiProvider` implementation (in `service/kpi/`). `DashboardServiceImpl` collects them all and returns `KpiTileDto` lists. Add a new tile by implementing `KpiProvider` and registering it as a Spring bean — all queries must be union-scoped.

### Database

`spring.jpa.hibernate.ddl-auto=create-drop` — schema is **dropped and recreated on every startup**. There is no migration tool. `InitDataConfig` (`CommandLineRunner`) seeds two test unions and demo data on each start.

**Local dev credentials** (in `application.properties`):
- DB: `jdbc:mysql://localhost:3306/civitas_db2`, user `springuser2` / `Spring123!`
- Login: `apo` / `apo` (Civitas Demo Union), `admin_gent` / `gent123` (Student Union Ghent)

### Security

- All routes require authentication except `/`, `/login**`, `/css/**`, `/js/**`, `/error`, `/stripe/webhook`
- CSRF enabled everywhere except `/stripe/webhook`
- No `@PreAuthorize` annotations — union authorization is done manually in service methods

## Adding a New Tenant-Scoped Entity

1. Add `@ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "union_id", nullable = false) private Union union;`
2. Repository methods must all accept `Union union` as a filter parameter
3. Service create method: `entity.setUnion(getCurrentUserUnion())`
4. Service read/delete: call a `verifyXxxBelongsToUnion()` guard before any mutation

# Civitas Project Guide

## Project Overview
Civitas is een Multi-Tenant SaaS platform voor het beheer van verenigingen en vakbonden.
- **Kernfocus:** Strikte data-isolatie tussen unies (multi-tenancy).
- **Tech Stack:** Java 21, Spring Boot 3, Hibernate/JPA, MySQL, Spring Security, Stripe, i18n.

## Development Rules
- **Taal:** Code en comments in het Engels. Gebruikers-interface en properties in NL/EN/TR.
- **Architectuur:** Respecteer altijd de `UnionId` filters in repositories en services voor security.
- **Testing:** Schrijf JUnit 5/Mockito tests voor elke bugfix of nieuwe feature.
- **Workflow:** Toon altijd een plan voordat je bestanden wijzigt. Draai `./mvnw compile` na wijzigingen.

## Active TODO List
### i18n & UX
- [x] Notificaties vertalen naar Turks en Engels (messages_tr.properties, messages_en.properties).
- [x] Accounting page vertalen naar Turks en Engels.
- [x] Pagetitle toevoegen op de Accounting page.
- [x] Transactions page: Filter direct toepassen bij selectie (geen enter nodig).

### Bugfixes
- [x] Transactions pagination bug oplossen.
- [x] Invoice upload debuggen: Geuploade factuur verschijnt niet in de image preview.
- [x] 'Mark all notifications as read' 404 error fixen http://localhost:8080/notifications/mark-all-read.

### New Features & Scaling
- [x] Bulk member import: Excel upload functionaliteit implementeren.
- [ ] Recurring payments onderzoek en implementatie (Stripe subs).
- [ ] Security audit: Validatie van alle betalings-flows en data-isolatie.

### Production Prep
- [ ] Voorbereiding hosting (Railway/Docker).
- [ ] Twilio & Stripe productie-configuratie.