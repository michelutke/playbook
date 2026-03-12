---
template: handover
version: 0.1.0
status: DONE
---
# Handover: Super Admin

## What Was Built

Web-only SuperAdmin panel (SvelteKit) + Ktor backend extension:
- Dashboard: total clubs, users, active events today, 7-day sign-up count
- Club management: list (search + status filter), create, edit, deactivate/reactivate, permanent delete (requires `confirm_name` body)
- Manager management: invite by email, list pending + active, remove
- Impersonation: start session → short-lived JWT as manager; client-side countdown; end session early
- Audit log: immutable, paginated, filterable; async CSV export with job polling
- Billing summary: per-club active member count × CHF/member/year
- User search: find user by name/email across clubs
- All routes prefixed `/api/sa/` guarded by `requireSuperAdmin()`

## Architecture Decisions

| Decision | Outcome |
|---|---|
| SA web tech | SvelteKit — decided independently of ADR-001; SA panel is always web, not CMP |
| Auth | `super_admin = true` flag on `users`; no separate SA user table; set manually (no registration) |
| Impersonation | Separate short-lived JWT (`sub` = manager, `impersonated_by` = SA); regular endpoints see manager identity |
| Audit log immutability | DB role has no `UPDATE`/`DELETE` on `audit_log`; Ktor interceptor writes all SA route activity |
| Soft delete | Clubs have `deleted_at` guard before permanent delete |
| CSV export | Async: `POST` queues job, `GET` polls → returns download URL |
| Billing | Read-only; rate configurable server-side; annual per active member |
| Audit retention | Scheduled job purges records older than 2 years |

## Key Files

```
backend/src/main/kotlin/com/playbook/
  routes/SuperAdminRoutes.kt
  plugins/AuditPlugin.kt     — Ktor interceptor on /api/sa/ routes
  jobs/AuditExportJob.kt
  jobs/AuditRetentionJob.kt

admin/                       — SvelteKit project
  src/routes/
    dashboard/
    clubs/
    audit-log/
    billing/
  src/lib/
    impersonation.ts         — JWT countdown timer
    auth.ts                  — SA session guard
```

## Migrations

`backend/src/main/resources/db/migrations/V16__*.sql`
- Tables: `audit_log`, `impersonation_sessions`, `club_managers`
- Columns: `super_admin` flag on `users`

## Known Limitations

- SA is manually provisioned (`super_admin` flag set in DB) — no self-service SA creation
- Impersonation session is client-managed countdown (JWT `exp`); backend validates expiry on each request
- Async export stores file locally — not suitable for multi-instance deploy without shared storage
- Billing rate is server-side config, not DB-driven; no invoice generation

## Upstream Dependencies

- team-management: relies on `clubs`, `teams`, `team_memberships`
- All features: audit log captures cross-feature SA actions
