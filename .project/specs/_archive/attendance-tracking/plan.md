---
template: plan
version: 0.1.0
gate: READY
---
# Implementation Plan: Attendance Tracking

> **Depends on:** team-management (users, memberships), event-scheduling (events, subgroups)
> **Implement third** — requires events to exist before attendance can be tracked.

---

## Phase 1 — Database Schema

- [ ] Create `attendance_responses` table (event_id, user_id, status ENUM, reason, abwesenheit_rule_id, manual_override, responded_at, updated_at) — PK `(event_id, user_id)`
- [ ] Create `attendance_records` table (event_id, user_id, status ENUM, note, set_by, set_at, previous_status, previous_set_by) — PK `(event_id, user_id)`
- [ ] Create `abwesenheit_rules` table (id, user_id, preset_type ENUM, label, rule_type ENUM, weekdays, start_date, end_date, created_at, updated_at)
- [ ] Add indexes: `attendance_responses(user_id, event_id)`, `attendance_records(event_id)`, `events(start_at)` (already from event-scheduling)

## Phase 2 — Ktor Backend

### Attendance response endpoints
- [ ] `GET /events/{id}/attendance` — all responses for event (team view, grouped by status)
- [ ] `GET /events/{id}/attendance/me` — own current response
- [ ] `PUT /events/{id}/attendance/me` — upsert response; validate: `unsure` requires `reason` (400); reject after deadline (409); set `manual_override = true` if `abwesenheit_rule_id` present on existing row

### Abwesenheit endpoints
- [ ] `GET /users/me/abwesenheit` — list own rules
- [ ] `POST /users/me/abwesenheit` — create rule; enqueue backfill job; return `{ job_id }`
- [ ] `PUT /users/me/abwesenheit/{ruleId}` — update rule; enqueue backfill job
- [ ] `DELETE /users/me/abwesenheit/{ruleId}` — delete rule; past `declined-auto` entries unchanged
- [ ] `GET /users/me/abwesenheit/backfill-status` — poll job status: `pending | done | failed`

### Coach check-in endpoints
- [ ] `GET /events/{id}/check-in` — full list with current responses + records; coach only
- [ ] `PUT /events/{id}/check-in/{userId}` — set `present | absent | excused` + optional note; capture previous state in `attendance_records`

### Raw attendance data (for client-side stats)
- [ ] `GET /users/{userId}/attendance?from=&to=` — raw response + record rows; filtered by date range
- [ ] `GET /teams/{teamId}/attendance?from=&to=` — same for all team members

## Phase 3 — Background Jobs

### Abwesenheit backfill job
- [ ] Implement `backfillAbwesenheit(ruleId)` — query all future team events matching the rule; upsert `attendance_responses(status = declined-auto, abwesenheit_rule_id)` skipping rows where `manual_override = true`
- [ ] Run as coroutine launched from route handler (non-blocking); update job state in `abwesenheit_backfill_jobs` table (`pending → processing → done | failed`)
- [ ] On rule delete: no backfill; existing `declined-auto` entries preserved

### Post-event auto-present job
- [ ] Periodic coroutine (every 5 min): query events where `end_at` passed in last 5-min window
- [ ] For each event where `team.check_in_enabled = false`: upsert `attendance_records(status = present, set_by = system)` for all `confirmed` responses
- [ ] For events where `check_in_enabled = true`: no-op (coach submits manually)

### Trigger on event creation
- [ ] When event created: check all team members' active abwesenheit rules; insert `declined-auto` entries for matches

## Phase 4 — KMP Domain Layer

- [ ] Define `AttendanceRepository` interface + impl (fetch responses, upsert own response)
- [ ] Define `AbwesenheitRepository` interface + impl (list, create, update, delete, poll backfill status)
- [ ] **Offline mutation queue** in SQLDelight:
  - Schema: `attendance_mutation_queue` (id, type, payload JSON, created_at)
  - On network response: dequeue and apply in order
  - On reconnect: flush queue to API; handle 409 (deadline) by reverting optimistic state + show error snackbar
- [ ] **Optimistic UI**: update local SQLDelight cache immediately; revert on sync failure
- [ ] Deadline check: `Clock.System.now() > event.responseDeadline` in shared domain; disable response UI
- [ ] **Stats aggregation** (client-side): given raw attendance rows, compute `presence_pct`, `training_pct`, `match_pct` per filter — pure function in shared domain, no server endpoint

## Phase 5 — Mobile UI (CMP)

- [ ] **S1 Event Detail — My Attendance card** — three inline tap targets (Confirmed / Declined / Unsure); disabled after deadline with "Deadline reached" label; optimistic update
- [ ] **S2 Begründung Sheet** — bottom sheet for Declined (optional) and Unsure (mandatory); inline validation
- [ ] **S3 Attendance List Screen** — sections by status; expandable rows (Begründung + auto-decline indicator ⟳); coach rows tappable for override
- [ ] **S6 My Absences Screen** — empty state; list with type icon + label + date chip; swipe-delete; tap to edit
- [ ] **S7 Add/Edit Absence Sheet** — preset icon grid; Recurring | Period toggle; weekday grid / date range picker
- [ ] **S8 Player Statistics Screen** — filter bar (event type + date range); progress rings/bars for stats (computed in domain layer)
- [ ] **S9 Team Statistics Screen** — filter bar; player list with presence % bar; tap → S8
- [ ] Backfill snackbar: "Applying absence rules…" (persistent) → "Absence rules applied" (3s) or "Failed — tap to retry" (persistent)
- [ ] Status badge variants: confirmed (green dot), declined (red dot), unsure (yellow dot), declined-auto (dashed + ⟳)
- [ ] Offline indicator: queue badge / toast when syncing on reconnect
