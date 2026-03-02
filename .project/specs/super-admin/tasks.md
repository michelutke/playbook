---
template: tasks
version: 0.1.0
gate: READY GO
---
# Tasks: Super Admin

> **Depends on:** team-management (clubs, users tables). Largely independent of other features.

## Phase 1 — Database Schema

| ID | Task | Deps |
|---|---|---|
| ✅ SA-001 | Add `super_admin BOOLEAN DEFAULT false` column to `users` table migration | — |
| ✅ SA-002 | Create `audit_log` table — grant app DB role no `UPDATE`/`DELETE` on this table | ✅ SA-001 |
| ✅ SA-003 | Create `impersonation_sessions` table | ✅ SA-001 |
| ✅ SA-004 | Create `export_jobs` table (id, type, actor_id, status, filters JSONB, result_path, created_at, completed_at) | ✅ SA-001 |
| ✅ SA-005 | Add indexes: `audit_log(actor_id)`, `audit_log(created_at)`, `impersonation_sessions(superadmin_id, ended_at)` | ✅ SA-002, SA-003 |

## Phase 2 — Backend Auth & Middleware

| ID | Task | Deps |
|---|---|---|
| ✅ SA-006 | Configure `jwt("super-admin")` auth — validate JWT + query `users.super_admin = true` from DB (not JWT claim) | ✅ SA-001 |
| ✅ SA-007 | `requireSuperAdmin()` middleware — 403 if flag false | ✅ SA-006 |
| ✅ SA-008 | Configure `jwt("impersonation")` auth — separate JWT validator with `impersonated_by` + `impersonation_session_id` custom claims; max expiry 3600s | ✅ SA-006 |
| ✅ SA-009 | `mintImpersonationToken(saId, managerId, sessionId)` — signs short-lived JWT with impersonation claims | ✅ SA-008 |
| ✅ SA-010 | Audit logging Ktor plugin — intercepts all `/api/sa/*` routes; writes `audit_log` row with before/after snapshot, IP, user agent | ✅ SA-002, SA-007 |
| ✅ SA-011 | Detect impersonation JWT in audit plugin — populate `impersonated_as` + `impersonation_session_id` from claims | ✅ SA-010, SA-008 |

## Phase 2 — Dashboard & Stats

| ID | Task | Deps |
|---|---|---|
| ✅ SA-012 | `GET /sa/stats` — total clubs, users, active events today, sign-ups (7d) | ✅ SA-007 |

## Phase 2 — Club Endpoints

| ID | Task | Deps |
|---|---|---|
| ✅ SA-013 | `GET /sa/clubs` — list with status filter + search | ✅ SA-007 |
| ✅ SA-014 | `POST /sa/clubs` — create club + queue manager invites (reuses invite flow from team-management) | ✅ SA-007 |
| ✅ SA-015 | `GET /sa/clubs/{id}` — detail with managers list (active + pending) | ✅ SA-007 |
| ✅ SA-016 | `PATCH /sa/clubs/{id}` — edit name/metadata | ✅ SA-007 |
| ✅ SA-017 | `POST /sa/clubs/{id}/deactivate` — set `status = inactive`; invalidate all club sessions | ✅ SA-007 |
| ✅ SA-018 | `POST /sa/clubs/{id}/reactivate` — set `status = active` | ✅ SA-007 |
| ✅ SA-019 | `DELETE /sa/clubs/{id}` — validate `confirm_name` body field server-side; permanent delete | ✅ SA-007 |

## Phase 2 — Manager Endpoints

| ID | Task | Deps |
|---|---|---|
| ✅ SA-020 | `GET /sa/clubs/{id}/managers` — list active + pending `club_managers` rows | ✅ SA-007 |
| ✅ SA-021 | `POST /sa/clubs/{id}/managers` — invite by email; reuse `generateToken()` + email from team-management | ✅ SA-007 |
| ✅ SA-022 | `DELETE /sa/clubs/{id}/managers/{userId}` — remove manager | ✅ SA-007 |

## Phase 2 — Impersonation Endpoints

| ID | Task | Deps |
|---|---|---|
| ✅ SA-023 | `POST /sa/clubs/{id}/managers/{userId}/impersonate` — create `impersonation_sessions` row; call `mintImpersonationToken`; return `{ token, session_id, expires_at }` | ✅ SA-009, SA-007 |
| ✅ SA-024 | `POST /sa/impersonation/{sessionId}/end` — set `ended_at = now()` | ✅ SA-007 |

## Phase 2 — User Search

| ID | Task | Deps |
|---|---|---|
| ✅ SA-025 | `GET /sa/users/search?q=` — search by name/email; return club memberships only; no player/personal data | ✅ SA-007 |

## Phase 2 — Audit Log Endpoints

| ID | Task | Deps |
|---|---|---|
| ✅ SA-026 | `GET /sa/audit-log` — paginated; filterable by actor, action, date range | ✅ SA-002, SA-007 |
| ✅ SA-027 | `GET /sa/audit-log/{id}` — single entry full payload | ✅ SA-002, SA-007 |
| ✅ SA-028 | `POST /sa/audit-log/export` — enqueue async CSV job; return `{ job_id }` | ✅ SA-004, SA-007 |
| ✅ SA-029 | `GET /sa/audit-log/export/{jobId}` — poll `export_jobs.status`; return download URL when `done` | ✅ SA-028 |

## Phase 2 — Billing Endpoints

| ID | Task | Deps |
|---|---|---|
| ✅ SA-030 | `GET /sa/clubs/{id}/members` — active member list + count | ✅ SA-007 |
| ✅ SA-031 | `GET /sa/billing/summary` — all clubs × active member count × annual CHF; rate from server config | ✅ SA-007 |

## Phase 3 — Async CSV Export Job

| ID | Task | Deps |
|---|---|---|
| ✅ SA-032 | Background coroutine launched from `POST /sa/audit-log/export`: query filtered rows → write CSV to `exports/` dir → update `export_jobs.status = done` + `result_path` | ✅ SA-004 |
| ✅ SA-033 | Ktor route `GET /sa/audit-log/export/{jobId}` serves file via `call.respondFile()` when status `done` | ✅ SA-032 |
| ✅ SA-034 | Cleanup coroutine: delete export files + job rows older than 1 hour | ✅ SA-032 |

## Phase 4 — Svelte Admin Panel

### Project setup
| ID | Task | Deps |
|---|---|---|
| ✅ SA-035 | Init SvelteKit 2 + Svelte 5 project; configure SPA mode (`ssr = false, prerender = false`) | — |
| ✅ SA-036 | Add Tailwind CSS 4 + shadcn-svelte component library | ✅ SA-035 |
| ✅ SA-037 | Route guard: `+layout.server.ts` checks SA JWT cookie; redirect to `/login` if missing/invalid | ✅ SA-035, SA-006 |

### Auth
| ID | Task | Deps |
|---|---|---|
| ✅ SA-038 | SA login page — email + password → exchange for JWT → store in httpOnly cookie | ✅ SA-037 |
| ✅ SA-039 | Logout — clear cookie | ✅ SA-038 |

### Screens
| ID | Task | Deps |
|---|---|---|
| ✅ SA-040 | **Dashboard** — metric cards (clubs, users, events today, sign-ups 7d) + recent audit feed | ✅ SA-012 |
| ✅ SA-041 | **Clubs list** — search bar + status filter tabs + sortable table + "New Club" button | ✅ SA-013 |
| ✅ SA-042 | **Club detail** — summary + Managers section (active/pending rows, invite/remove actions) + Danger Zone accordion | ✅ SA-015, SA-020 |
| ✅ SA-043 | **Create club modal** — name + metadata + repeatable manager email rows | ✅ SA-014 |
| ✅ SA-044 | **Invite manager sheet** — email input | ✅ SA-021 |
| ✅ SA-045 | **Deactivate/Reactivate club** — confirm dialog with consequence text | ✅ SA-017, SA-018 |
| ✅ SA-046 | **Delete club modal** — full-page modal + type-to-confirm; enabled only on name match | ✅ SA-019 |
| ✅ SA-047 | **Impersonation flow** — start from manager row; amber persistent banner with countdown; "Exit" button | ✅ SA-023, SA-024 |
| ✅ SA-048 | **Impersonation timer** — client-side countdown from JWT `exp`; 5-min warning toast; auto-redirect on expiry | ✅ SA-047 |
| ✅ SA-049 | **User search** — global search bar in header; results list; user detail drawer (memberships only) | ✅ SA-025 |
| ✅ SA-050 | **Audit log** — filterable table; row click → detail drawer with JSON payload; "Export CSV" button | ✅ SA-026, SA-027 |
| ✅ SA-051 | **Audit log CSV export** — "Export" → poll `export_jobs` status → auto-download when ready | ✅ SA-028, SA-029 |
| ✅ SA-052 | **Billing summary** — read-only table: club · member count · annual CHF | ✅ SA-031 |
