# TODO

Working task list, separate from CLAUDE.md. Reference this manually when picking up work — it is not part of the always-loaded context.

## Stripe & payments

- [ ] Stripe Connect Express onboarding flow (association clicks "Link bank account" → standard Stripe Express flow, no dev keys touched)
- [ ] `application_fee_amount` per payment: `max(3% of amount, €0.60)`, capped at €15 for event payments only
- [ ] Decoupled fee architecture (`on_behalf_of`) so Stripe's own processing cost is deducted from the connected account, not from our margin
- [ ] Stripe Billing subscription for the fixed monthly fee (€60 list / €29 founding)
- [ ] **Stripe webhook signature verification** — critical, currently missing. Without this, a forged request could mark a payment as "paid" without any real transaction.
- [ ] Recurring payments research (Stripe Subscriptions) for the fixed monthly fee specifically

## Security (see full checklist doc for detail)

- [ ] Authorization check on every API endpoint itself, not just hidden in the UI
- [ ] Enforce read-only vs. write role at the backend/service layer, not only in the frontend
- [ ] File upload validation (accounting documents): type/size check, store outside webroot, no executables
- [ ] Verify CSRF/XSS/SQL-injection protections haven't been accidentally disabled anywhere
- [ ] Dependency vulnerability scan before going live (`mvn dependency-check` or Dependabot)
- [ ] No sensitive data (IBANs, tokens, passwords) in plaintext logs
- [ ] Rate limiting / brute-force protection on the login page
- [ ] Full multi-tenant isolation audit: actively try to break isolation between two test unions, don't just assume `union` scoping is correct everywhere

## Features (from board feedback)

- [ ] Role split in board accounts: read-only vs. write access
- [ ] Accounting: fixed, limited category list when booking a cost/income (e.g. Rent, Catering, Materials, Insurance, Events, Other)
- [ ] Accounting: "download all documents for category X as zip" per quarter
- [ ] Automated, regular database backups
- [ ] Backup restore test — an untested backup is an unproven backup
- [ ] Introduce Flyway for database migrations, replacing `ddl-auto=update` — current setting works but risks silent data loss on schema changes (e.g. column renames); becomes more important as the accounting categories and role features add schema complexity

## Production prep

- [ ] Migrate off home server to real cloud hosting
- [ ] HTTPS everywhere (verify explicitly during the hosting migration)
- [ ] Server hardening: firewall, SSH key-only, root login disabled
- [ ] Twilio & Stripe production configuration (currently test/dev keys)
- [ ] Monitoring/alerting: know within minutes if the server, Stripe, or WhatsApp integration goes down

## On hold — do not build yet

- [ ] Payments to non-members: blocked until the Stripe ToS/donation classification is fully resolved (higher risk of re-triggering a compliance flag)
- [ ] itsme / eID integration: dropped, not worth the partner-agreement overhead — existing signup form already solves the "add a member quickly" problem

## Role-based access (ADMIN / VIEWER) follow-ups

- [ ] **AJAX write calls answer a VIEWER with an HTML error page, not a JSON 403.**
  Write endpoints are protected server-side with `@PreAuthorize("hasRole('ADMIN')")`, so a VIEWER
  can never change data. But a denied request is handled by `accessDeniedPage(...)` in
  `SecurityConfig`: the response is a `403` whose body is the HTML page from `CustomErrorController`.
  The `fetch()` callers (`accounting.js`, `members.js`, `memberDetails.js`, the create-event script
  in `events.html`) expect JSON, so they fail with a generic "something went wrong" instead of a
  clear "you don't have permission". Only reachable by bypassing the UI (write buttons are hidden
  for VIEWERs via `sec:authorize`), so it is cosmetic, not a security issue.
  Fix idea: an `AccessDeniedHandler` that returns `403` + a small JSON body for `/api/**` and
  `/transactions/**` (keep the HTML page for browser navigation), and have those scripts show a
  translated "no permission" message on `403`.
- [ ] `docs/migrations/2026-09-add-viewer-role.sql` is a manual stopgap for the `role` ENUM column
  (`ddl-auto=update` never alters existing columns). **Back up the database before running it.**
  Replace it with a proper Flyway migration when Flyway is introduced (see above).
- [ ] Testcontainers 1.21.x can't talk to Docker Engine 29+ ("client version 1.32 is too old").
  The MySQL integration tests are skipped in that case; run them with
  `./mvnw test -DargLine="-Dapi.version=1.44"` until Testcontainers is upgraded.
