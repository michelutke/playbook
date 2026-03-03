---
template: plan
version: 0.1.0
gate: READY
---
# Implementation Plan: Super Admin

> **Depends on:** team-management (clubs, users tables already exist)
> **Largely independent** — can be built in parallel with attendance/notifications.

---

## Phase 1 — Database Schema

- [ ] Add `super_admin BOOLEAN DEFAULT false` column to `users` table; set manually for SA accounts
- [ ] Create `audit_log` table (id, actor_id, action, target_type, target_id, payload JSONB, impersonated_as, impersonation_session_id, created_at) — DB role has no UPDATE/DELETE on this table
- [ ] Create `impersonation_sessions` table (id, superadmin_id, manager_id, club_id, started_at, expires_at, ended_at)
- [ ] Create `export_jobs` table (id, type, actor_id, status ENUM, filters JSONB, result_path, created_at, completed_at)
- [ ] Add indexes: `audit_log(actor_id)`, `audit_log(created_at)`, `impersonation_sessions(superadmin_id, ended_at)`

## Phase 2 — Ktor Backend

### Auth middleware
- [ ] `requireSuperAdmin()`: validate JWT + query `users.super_admin = true` from DB (not JWT claim); 403 if not SA
- [ ] Configure `jwt("impersonation")` auth with custom claims (`impersonated_by`, `impersonation_session_id`, exp = 1h)
- [ ] `mintImpersonationToken(saId, managerId, sessionId)`: signs short-lived JWT with impersonation claims

### Audit logging interceptor
- [ ] Ktor plugin on all `/api/sa/` routes: intercept response; write `audit_log` row with before/after snapshot, request IP, user agent
- [ ] Detect impersonation JWT: populate `impersonated_as` + `impersonation_session_id` from claims

### Dashboard
- [ ] `GET /sa/stats` — total clubs, users, active events today, sign-ups (7d)

### Club endpoints
- [ ] `GET /sa/clubs` — list with status filter + search
- [ ] `POST /sa/clubs` — create club + queue manager invites
- [ ] `GET /sa/clubs/{id}` — detail with managers list
- [ ] `PATCH /sa/clubs/{id}` — edit name/metadata
- [ ] `POST /sa/clubs/{id}/deactivate` / `reactivate`
- [ ] `DELETE /sa/clubs/{id}` — validate `confirm_name` body field server-side; permanent delete

### Manager endpoints
- [ ] `GET /sa/clubs/{id}/managers` — list active + pending
- [ ] `POST /sa/clubs/{id}/managers` — invite by email; reuse invite flow from team-management
- [ ] `DELETE /sa/clubs/{id}/managers/{userId}` — remove manager

### Impersonation endpoints
- [ ] `POST /sa/clubs/{id}/managers/{userId}/impersonate` — create `impersonation_sessions` row; mint token; return `{ token, session_id, expires_at }`
- [ ] `POST /sa/impersonation/{sessionId}/end` — set `ended_at = now()`

### User search
- [ ] `GET /sa/users/search?q=` — search by name/email; return club memberships only (no player data)

### Audit log endpoints
- [ ] `GET /sa/audit-log` — paginated + filterable (actor, action, date range)
- [ ] `GET /sa/audit-log/{id}` — single entry detail
- [ ] `POST /sa/audit-log/export` — enqueue async CSV job; return `{ job_id }`
- [ ] `GET /sa/audit-log/export/{jobId}` — poll status; return download path when `done`

### Billing endpoints
- [ ] `GET /sa/clubs/{id}/members` — active member list + count
- [ ] `GET /sa/billing/summary` — all clubs with active member counts + annual CHF (rate from server config)

## Phase 3 — Async CSV Export Job

- [ ] `POST /sa/audit-log/export` launches coroutine: query audit log with filters → write CSV to `exports/` dir → update `export_jobs.status = done`, set `result_path`
- [ ] `GET .../export/{jobId}` returns `{ status }` or `{ status: "done", download_url }` when complete
- [ ] Ktor route serves file: `call.respondFile(File(resultPath))`
- [ ] Cleanup job: delete export files older than 1 hour

## Phase 4 — Svelte Admin Panel

### Project setup
- [ ] Init SvelteKit 2 + Svelte 5 project (Vite); configure for SPA mode (`ssr = false`)
- [ ] Add Tailwind CSS 4; configure shadcn-svelte or Flowbite-Svelte for components
- [ ] Route guard: `+layout.server.ts` checks SA session cookie; redirect to `/login` if invalid

### Auth
- [ ] SA login page: email + password → exchange for SA JWT → store in httpOnly cookie
- [ ] Logout: clear cookie + invalidate session

### Screens
- [ ] **Dashboard** — metric cards (clubs, users, events today, sign-ups 7d) + recent audit feed
- [ ] **Clubs list** — search + status filter tabs; sortable table; "New Club" button
- [ ] **Club detail** — summary section + Managers section (list with invite/remove actions + pending badges) + Danger Zone accordion
- [ ] **Create club modal** — name + metadata + manager email(s) (repeatable rows)
- [ ] **Invite manager sheet** — email input
- [ ] **Delete club confirmation** — full-page modal + type-to-confirm
- [ ] **Impersonation** — start from manager row; amber banner persists across all pages with countdown timer; "Exit" button
- [ ] **User search** — global search bar; results list; user detail drawer
- [ ] **Audit log** — filterable table; tap row → detail drawer with JSON payload; "Export CSV" button → poll → download
- [ ] **Billing summary** — read-only table: club · members · annual CHF

### Impersonation timer
- [ ] Client-side countdown from JWT `exp` claim; no polling
- [ ] 5-min warning toast
- [ ] Auto-redirect to SA context on expiry
