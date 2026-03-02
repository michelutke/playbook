---
template: tech
version: 0.1.0
gate: READY
---
# Tech Spec: Super Admin

## Platform Scope

SuperAdmin panel is **web-only** — independent of ADR-001 (CMP Web vs Svelte).
**Decision: Svelte** for the SA panel in all scenarios.

The KMP/CMP mobile app has no SA functionality and is out of scope here.

---

## Data Model

### `clubs`
| Column | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `name` | TEXT NOT NULL | |
| `status` | ENUM(`active`, `inactive`) | default `active` |
| `metadata` | JSONB | sport type, location, notes |
| `created_at` | TIMESTAMPTZ | |
| `deleted_at` | TIMESTAMPTZ NULLABLE | soft-delete guard before permanent delete |

### `club_managers`
| Column | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `club_id` | UUID FK → clubs | |
| `user_id` | UUID FK → users NULLABLE | null until invite accepted |
| `invited_email` | TEXT NOT NULL | |
| `status` | ENUM(`pending`, `active`) | |
| `invited_by` | UUID FK → users (SA) | |
| `invited_at` | TIMESTAMPTZ | |
| `accepted_at` | TIMESTAMPTZ NULLABLE | |

Unique constraint: `(club_id, invited_email)`.

### `audit_log`
| Column | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `actor_id` | UUID FK → users | always the SuperAdmin |
| `action` | TEXT NOT NULL | e.g. `club.create`, `manager.remove`, `club.impersonate` |
| `target_type` | TEXT | e.g. `club`, `user`, `club_manager` |
| `target_id` | UUID NULLABLE | |
| `payload` | JSONB | full before/after snapshot |
| `impersonated_as` | UUID FK → users NULLABLE | |
| `impersonation_session_id` | UUID NULLABLE | |
| `created_at` | TIMESTAMPTZ | |

**Immutability:** app DB role has no `UPDATE`/`DELETE` on `audit_log`.
**Retention:** scheduled job purges records older than 2 years.

### `impersonation_sessions`
| Column | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `superadmin_id` | UUID FK → users | |
| `manager_id` | UUID FK → users | |
| `club_id` | UUID FK → clubs | |
| `started_at` | TIMESTAMPTZ | |
| `expires_at` | TIMESTAMPTZ | `started_at + 1 hour` |
| `ended_at` | TIMESTAMPTZ NULLABLE | |

---

## API — Ktor Backend

All routes prefixed `/api/sa/` and guarded by `requireSuperAdmin()` middleware.

### Dashboard
| Method | Path | Description |
|---|---|---|
| GET | `/sa/stats` | Total clubs, users, active events today, sign-ups (7d) |

### Clubs
| Method | Path | Description |
|---|---|---|
| GET | `/sa/clubs` | List clubs (status filter, search) |
| POST | `/sa/clubs` | Create club + queue manager invites |
| GET | `/sa/clubs/{id}` | Club detail |
| PATCH | `/sa/clubs/{id}` | Edit name/metadata |
| POST | `/sa/clubs/{id}/deactivate` | Deactivate |
| POST | `/sa/clubs/{id}/reactivate` | Reactivate |
| DELETE | `/sa/clubs/{id}` | Permanent delete (requires `confirm_name` body field) |

### Managers
| Method | Path | Description |
|---|---|---|
| GET | `/sa/clubs/{id}/managers` | List managers (active + pending) |
| POST | `/sa/clubs/{id}/managers` | Invite manager by email |
| DELETE | `/sa/clubs/{id}/managers/{userId}` | Remove manager |

### Impersonation
| Method | Path | Description |
|---|---|---|
| POST | `/sa/clubs/{id}/managers/{userId}/impersonate` | Start session → short-lived JWT |
| POST | `/sa/impersonation/{sessionId}/end` | End session early |

Impersonation JWT claims:
```json
{
  "sub": "<manager_user_id>",
  "role": "club_manager",
  "impersonated_by": "<superadmin_user_id>",
  "impersonation_session_id": "<session_id>",
  "exp": "<started_at + 3600>"
}
```

### Users
| Method | Path | Description |
|---|---|---|
| GET | `/sa/users/search?q=` | Search by name/email; returns club memberships only |

### Audit Log
| Method | Path | Description |
|---|---|---|
| GET | `/sa/audit-log` | Paginated list (filter: actor, action, date range) |
| GET | `/sa/audit-log/{id}` | Single entry detail |
| POST | `/sa/audit-log/export` | Queue async CSV export → returns `job_id` |
| GET | `/sa/audit-log/export/{jobId}` | Poll status; returns download URL when ready |

### Billing
| Method | Path | Description |
|---|---|---|
| GET | `/sa/clubs/{id}/members` | Active member list + count for a club |
| GET | `/sa/billing/summary` | All clubs with active member counts + annual amounts |

Billing period: **annual**. Rate configurable server-side (default 1.00 CHF/member/year).

`/sa/billing/summary` response shape:
```json
[
  {
    "club_id": "...",
    "club_name": "FC Example",
    "active_member_count": 42,
    "annual_billing_chf": 42.00
  }
]
```

---

## Security

- `super_admin` flag on `users` table; set manually (no registration path)
- SA login reuses main auth — `requireSuperAdmin()` asserts `users.super_admin = true`
- Impersonation tokens are separate short-lived JWTs distinct from the SA's own token
- Regular app endpoints receiving an impersonation JWT see `sub` as the manager; audit middleware detects `impersonated_by` claim
- Permanent club delete validated server-side against `confirm_name` (not a UI-only gate)

---

## Audit Logging

- Written server-side as a Ktor interceptor on all `/api/sa/` routes
- `payload` stores `{ before, after, request_ip, user_agent }`
- Impersonated actions record both `actor_id` (SA) and `impersonated_as` (manager)
- CSV export is async: POST queues job, GET polls until `status: ready`, then returns pre-signed download URL

---

## Email

- Own email service (self-hosted / internal SMTP)
- Used for: manager invite, invite resend
- One-time invite links signed with short-lived token; rendered server-side in Ktor

---

## Frontend — Svelte (SA Panel)

- Separate Svelte app; not part of the main CMP app
- Route guard: redirect to SA login if no valid SA session
- Impersonation timer: client-side countdown from JWT `exp`; no polling required
- Destructive actions use dedicated confirmation components (not `window.confirm`)
- Billing summary: read-only table — club · member count · annual CHF
