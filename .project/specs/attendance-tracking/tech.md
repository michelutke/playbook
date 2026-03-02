---
template: tech
version: 0.1.0
gate: READY
---
# Tech Spec: Attendance Tracking

## Platform Scope

- Mobile (iOS + Android): KMP shared domain/data + CMP UI
- Web: deferred per ADR-001
- Offline-first: all player mutations queue locally and sync on reconnect (NFR)

---

## Data Model

### `attendance_responses` (player self-reporting)
| Column | Type | Notes |
|---|---|---|
| `event_id` | UUID FK → events | |
| `user_id` | UUID FK → users | |
| `status` | ENUM(`confirmed`, `declined`, `unsure`, `declined-auto`, `no-response`) | default `no-response` |
| `reason` | TEXT NULLABLE | Begründung; mandatory when `unsure` |
| `abwesenheit_rule_id` | UUID FK → abwesenheit_rules NULLABLE | set when `declined-auto` |
| `manual_override` | BOOLEAN | true if player responded explicitly despite active rule |
| `responded_at` | TIMESTAMPTZ NULLABLE | null until first explicit response |
| `updated_at` | TIMESTAMPTZ | |

PK: `(event_id, user_id)`

### `attendance_records` (coach post-event check-in)
| Column | Type | Notes |
|---|---|---|
| `event_id` | UUID FK → events | |
| `user_id` | UUID FK → users | |
| `status` | ENUM(`present`, `absent`, `excused`) | |
| `note` | TEXT NULLABLE | coach note |
| `set_by` | UUID FK → users | coach who last edited |
| `set_at` | TIMESTAMPTZ | |
| `previous_status` | ENUM NULLABLE | for audit trail |
| `previous_set_by` | UUID FK → users NULLABLE | |

PK: `(event_id, user_id)`

### `abwesenheit_rules`
| Column | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `user_id` | UUID FK → users | |
| `preset_type` | ENUM(`holidays`, `injury`, `work`, `school`, `travel`, `other`) | |
| `label` | TEXT | pre-filled from preset; free-text for `other` |
| `rule_type` | ENUM(`recurring`, `period`) | |
| `weekdays` | SMALLINT[] NULLABLE | `0`=Mon … `6`=Sun; for `recurring` |
| `start_date` | DATE NULLABLE | for `period` |
| `end_date` | DATE NULLABLE | optional for both types |
| `created_at` | TIMESTAMPTZ | |
| `updated_at` | TIMESTAMPTZ | |

---

## Auto-decline Logic (Abwesenheit)

Backfill **always runs as a background job** — never blocks the request.

Two triggers:
1. **Rule created** — job generates `declined-auto` entries for all future matching team events
2. **Rule updated** — job re-evaluates and upserts/removes `declined-auto` entries for affected events

Rule matching:
- `recurring`: event date falls on matching weekday AND (`end_date` null OR event date ≤ `end_date`)
- `period`: event date within `[start_date, end_date]`

**Rule deleted:** existing `declined-auto` entries unchanged.

**Manual override:** player submits explicit response → `manual_override = true`; backfill skips entries where `manual_override = true`.

### Job status endpoint
| Method | Path | Description |
|---|---|---|
| GET | `/users/me/abwesenheit/backfill-status` | Returns `{ status: pending \| done \| failed }` |

Client polls after rule create/update and shows:
- **While pending**: snackbar "Applying absence rules…" (persistent, no dismiss)
- **On done**: snackbar "Absence rules applied" (auto-dismiss 3s)
- **On failed**: snackbar "Failed to apply rules — tap to retry" (persistent, action)

---

## Post-Event Auto-present Logic

Background job triggered when `event.end_time` passes:
- `check_in_enabled = false`: all `confirmed` responses → insert `attendance_records(status=present, set_by=system)`
- `check_in_enabled = true`: no auto-set; coach submits manually

---

## API — Ktor Backend

### Player Responses
| Method | Path | Description |
|---|---|---|
| GET | `/events/{id}/attendance` | All responses for event (team view) |
| GET | `/events/{id}/attendance/me` | Own current response |
| PUT | `/events/{id}/attendance/me` | Upsert own response; 409 after deadline |

`PUT` body: `{ status, reason? }`
Server rejects `unsure` without `reason` (400). Rejects any status after deadline (409).

### Abwesenheit
| Method | Path | Description |
|---|---|---|
| GET | `/users/me/abwesenheit` | List own rules |
| POST | `/users/me/abwesenheit` | Create rule → enqueues backfill job |
| PUT | `/users/me/abwesenheit/{ruleId}` | Update rule → enqueues backfill job |
| DELETE | `/users/me/abwesenheit/{ruleId}` | Delete rule; past `declined-auto` unchanged |
| GET | `/users/me/abwesenheit/backfill-status` | Job status: `pending \| done \| failed` |

### Coach Check-In
| Method | Path | Description |
|---|---|---|
| GET | `/events/{id}/check-in` | Full attendance list with responses + records |
| PUT | `/events/{id}/check-in/{userId}` | Set `present` / `absent` / `excused` + optional note |

`PUT` captures previous state for audit. Requires `coach` role on event's team.

### Raw Attendance Data (for client-side stats)
| Method | Path | Description |
|---|---|---|
| GET | `/users/{userId}/attendance?from=&to=` | Raw response + record rows for date range |
| GET | `/teams/{teamId}/attendance?from=&to=` | Same for all team members |

Client aggregates: `presence_pct`, `training_pct`, `match_pct` computed on-device from raw rows. No server-side stats endpoint.

---

## Offline Support (KMP)

- **Local store**: SQLDelight database in KMP shared module mirrors `attendance_responses` and `abwesenheit_rules`
- **Mutation queue**: pending PUT/POST/DELETE serialised to local queue table
- **Sync**: queue flushed on network reconnect; server responses reconcile local state
- **Optimistic UI**: local state updated immediately; reverted on server error with toast
- **Deadline enforcement**: client checks cached event deadline; server is authoritative (409 on late sync)

---

## Statistics (Client-side)

Raw attendance rows fetched from API; aggregation runs on-device in KMP shared domain layer.

Computed per user per filter (event type + date range):
- `presence_pct` = `present` records / total events
- `training_pct` = `present` at training events / total training events
- `match_pct` = `present` at match events / total match events

No server stats endpoint needed.

---

## Indexes

- `attendance_responses(user_id, event_id)`
- `attendance_records(event_id)`
- `events(team_id, start_time)` — for backfill range queries

---

## KMP Architecture

- `attendance` domain module: `AttendanceRepository`, `AbwesenheitRepository`
- Business logic (auto-decline matching, deadline check, stats aggregation) in shared KMP — not in Composables
- Platform-specific: CMP UI, push notification triggers (`expect/actual`)
