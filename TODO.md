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

## Production prep

- [ ] Migrate off home server to real cloud hosting
- [ ] HTTPS everywhere (verify explicitly during the hosting migration)
- [ ] Server hardening: firewall, SSH key-only, root login disabled
- [ ] Twilio & Stripe production configuration (currently test/dev keys)
- [ ] Monitoring/alerting: know within minutes if the server, Stripe, or WhatsApp integration goes down

## On hold — do not build yet

- [ ] Payments to non-members: blocked until the Stripe ToS/donation classification is fully resolved (higher risk of re-triggering a compliance flag)
- [ ] itsme / eID integration: dropped, not worth the partner-agreement overhead — existing signup form already solves the "add a member quickly" problem