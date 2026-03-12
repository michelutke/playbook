---
template: handover
version: 0.1.0
status: DONE
---
# Handover: Attendance Tracking

## What Was Built

Full attendance lifecycle backend + Android UI:
- Player self-reporting: `confirmed`, `declined`, `unsure` (requires Begründung), `declined-auto`, `no-response`
- Abwesenheit rules: `recurring` (weekday-based) and `period` (date range) auto-decline
- Abwesenheit backfill background job: generates `declined-auto` entries for future events on rule create/update
- Manual override: explicit player response overrides an active rule
- Post-event auto-present job: converts `confirmed` → `attendance_record(present)` when `check_in_enabled = false`
- Coach check-in: manual `present`/`absent`/`excused` with note; previous state captured for audit
- Client-side stats aggregation: raw rows fetched, `presence_pct`/`training_pct`/`match_pct` computed on-device
- Offline-first: SQLDelight local store + mutation queue; optimistic UI; server-authoritative for deadlines
- Android UI: attendance list, absences screen, stats screen, StatusBadge, BegrundungSheet, OfflineIndicator

## Architecture Decisions

| Decision | Outcome |
|---|---|
| Stats | No server-side endpoint; client aggregates raw `attendance_responses` + `records` rows |
| Backfill | Always async background job — never blocks the create/update request |
| Auto-present | Background job at `event.end_time`; skipped when `check_in_enabled = true` |
| Offline | SQLDelight queue table; server-authoritative on deadline (409 on late sync) |
| Optimistic UI | Local state updated immediately; reverted on error with snackbar "Couldn't save — tap to retry" |
| Backfill status | Polled via `GET /users/me/abwesenheit/backfill-status` — client shows persistent snackbar while pending |

## Key Files

```
backend/src/main/kotlin/com/playbook/
  routes/AttendanceRoutes.kt
  routes/AbwesenheitRoutes.kt
  routes/CheckInRoutes.kt
  jobs/AbwesenheitBackfillJob.kt
  jobs/AutoPresentJob.kt

shared/src/commonMain/.../domain/
  Attendance.kt, Abwesenheit.kt

shared/src/commonMain/.../repository/
  AttendanceRepository.kt, AbwesenheitRepository.kt

shared/src/commonMain/.../db/
  Attendance.sq   — SQLDelight schema

androidApp/src/.../ui/
  attendancelist/   — per-event response list + coach check-in
  absences/         — abwesenheit rule management
  stats/            — attendance statistics with filters
  components/StatusBadge.kt
  components/BegrundungSheet.kt
  components/OfflineIndicator.kt
```

## Migrations

`backend/src/main/resources/db/migrations/`
- V17: `abwesenheit_rules` table
- V18: `attendance_responses` table
- V19: `attendance_records` table
- V20: attendance-related indexes
- V21: `check_in_enabled` column on `events`

## Known Limitations

- Backfill job is in-process (Ktor coroutines); large rule sets on big teams could be slow
- No server-side stats endpoint — if web client added later, it must replicate aggregation logic or a stats endpoint must be added
- Deadline enforcement is client-cached; edge case if device clock is wrong (server is authoritative)
- Audit trail for check-in is `previous_status` on the record — not a separate audit_log table

## Upstream Dependencies

- event-scheduling: needs `events` table with `check_in_enabled` column
- team-management: needs `team_memberships` for coach guards and member fan-out
