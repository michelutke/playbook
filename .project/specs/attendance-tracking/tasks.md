---
template: tasks
version: 0.1.0
gate: READY_GO
---
# Tasks: Attendance Tracking

> **Depends on:** team-management (users, memberships), event-scheduling (events, subgroups)

---

## Phase 1 — Database Schema

- [ ] **T-001** Create `attendance_responses` table
  - **Acceptance:** Migration runs; composite PK `(event_id, user_id)`; all columns present with correct types/nullability; ENUM values match spec
  - **Files:** `backend/src/main/resources/db/migration/V*__create_attendance_responses.sql`

- [ ] **T-002** Create `attendance_records` table
  - **Acceptance:** Migration runs; composite PK `(event_id, user_id)`; `previous_status` / `previous_set_by` nullable for audit trail
  - **Files:** `backend/src/main/resources/db/migration/V*__create_attendance_records.sql`

- [ ] **T-003** Create `abwesenheit_rules` table + `abwesenheit_backfill_jobs` table
  - **Acceptance:** `abwesenheit_rules` has all columns; `backfill_jobs` has `(id, rule_id, status ENUM(pending|processing|done|failed))`
  - **Files:** `backend/src/main/resources/db/migration/V*__create_abwesenheit.sql`

- [ ] **T-004** Add indexes
  - **Acceptance:** Indexes on `attendance_responses(user_id, event_id)`, `attendance_records(event_id)`, `events(team_id, start_time)` exist in migration
  - **Files:** `backend/src/main/resources/db/migration/V*__add_attendance_indexes.sql`

---

## Phase 2 — Ktor Backend: Player Response Endpoints

- [ ] **T-005** `GET /events/{id}/attendance` — team view grouped by status
  - **Acceptance:** Returns all responses grouped by status; requires team membership; 404 if event not found
  - **Files:** `backend/src/main/kotlin/routes/AttendanceRoutes.kt`, `backend/src/main/kotlin/services/AttendanceService.kt`

- [ ] **T-006** `GET /events/{id}/attendance/me` — own response
  - **Acceptance:** Returns current user's response row; `no-response` default if no row exists
  - **Files:** `backend/src/main/kotlin/routes/AttendanceRoutes.kt`

- [ ] **T-007** `PUT /events/{id}/attendance/me` — upsert own response
  - **Acceptance:** Upserts row; `unsure` without `reason` → 400; submission after deadline → 409; sets `manual_override=true` if active `abwesenheit_rule_id` on existing row
  - **Files:** `backend/src/main/kotlin/routes/AttendanceRoutes.kt`, `backend/src/main/kotlin/services/AttendanceService.kt`

---

## Phase 2 — Ktor Backend: Abwesenheit Endpoints

- [ ] **T-008** `GET /users/me/abwesenheit` — list own rules
  - **Acceptance:** Returns all active rules for authenticated user; empty array if none
  - **Files:** `backend/src/main/kotlin/routes/AbwesenheitRoutes.kt`, `backend/src/main/kotlin/services/AbwesenheitService.kt`

- [ ] **T-009** `POST /users/me/abwesenheit` — create rule
  - **Acceptance:** Inserts rule; enqueues backfill job; returns `{ job_id }`; 400 on invalid payload
  - **Files:** `backend/src/main/kotlin/routes/AbwesenheitRoutes.kt`, `backend/src/main/kotlin/services/AbwesenheitService.kt`

- [ ] **T-010** `PUT /users/me/abwesenheit/{ruleId}` — update rule
  - **Acceptance:** Updates rule; enqueues new backfill job; 404 if rule not owned by user
  - **Files:** `backend/src/main/kotlin/routes/AbwesenheitRoutes.kt`

- [ ] **T-011** `DELETE /users/me/abwesenheit/{ruleId}` — delete rule
  - **Acceptance:** Deletes rule; existing `declined-auto` attendance_responses unchanged; 404 if not owned by user
  - **Files:** `backend/src/main/kotlin/routes/AbwesenheitRoutes.kt`

- [ ] **T-012** `GET /users/me/abwesenheit/backfill-status` — poll job status
  - **Acceptance:** Returns `{ status: pending | done | failed }` for latest job; 404 if no job exists
  - **Files:** `backend/src/main/kotlin/routes/AbwesenheitRoutes.kt`

---

## Phase 2 — Ktor Backend: Coach Check-In Endpoints

- [ ] **T-013** `GET /events/{id}/check-in` — full check-in list (coach only)
  - **Acceptance:** Returns all team members with current response + record; 403 if not coach role on event's team
  - **Files:** `backend/src/main/kotlin/routes/CheckInRoutes.kt`, `backend/src/main/kotlin/services/CheckInService.kt`

- [ ] **T-014** `PUT /events/{id}/check-in/{userId}` — set check-in status
  - **Acceptance:** Upserts `attendance_records`; captures `previous_status` + `previous_set_by`; optional note; requires coach role; 403 otherwise
  - **Files:** `backend/src/main/kotlin/routes/CheckInRoutes.kt`, `backend/src/main/kotlin/services/CheckInService.kt`

---

## Phase 2 — Ktor Backend: Raw Attendance Data

- [ ] **T-015** `GET /users/{userId}/attendance?from=&to=`
  - **Acceptance:** Returns response + record rows for user in date range; self or coach access; 403 otherwise; empty array if no data
  - **Files:** `backend/src/main/kotlin/routes/AttendanceRoutes.kt`

- [ ] **T-016** `GET /teams/{teamId}/attendance?from=&to=`
  - **Acceptance:** Returns rows for all team members in date range; coach/manager access only
  - **Files:** `backend/src/main/kotlin/routes/AttendanceRoutes.kt`

---

## Phase 3 — Background Jobs

- [ ] **T-017** Abwesenheit backfill job
  - **Acceptance:** Queries all future team events matching rule; upserts `declined-auto` rows; skips `manual_override=true` rows; updates job status `pending→processing→done|failed`; runs non-blocking (coroutine)
  - **Files:** `backend/src/main/kotlin/jobs/AbwesenheitBackfillJob.kt`

- [ ] **T-018** Post-event auto-present job
  - **Acceptance:** Periodic coroutine every 5 min; finds events where `end_at` passed in last 5-min window; for `check_in_enabled=false`: inserts `attendance_records(status=present, set_by=system)` for all `confirmed` responses; no-op for `check_in_enabled=true`
  - **Files:** `backend/src/main/kotlin/jobs/AutoPresentJob.kt`

- [ ] **T-019** Trigger backfill on event creation
  - **Acceptance:** When event created, checks all team members' active abwesenheit rules; inserts `declined-auto` entries for matches; runs async (non-blocking)
  - **Files:** `backend/src/main/kotlin/services/EventService.kt` (hook in existing create logic)

---

## Phase 4 — KMP Domain Layer

- [ ] **T-020** `AttendanceRepository` interface + impl
  - **Acceptance:** Interface in `commonMain`; impl fetches responses via API, reads/writes SQLDelight cache; exposes `Flow<AttendanceResponse>` for reactive UI
  - **Files:** `shared/src/commonMain/kotlin/attendance/AttendanceRepository.kt`, `shared/src/commonMain/kotlin/attendance/AttendanceRepositoryImpl.kt`

- [ ] **T-021** `AbwesenheitRepository` interface + impl
  - **Acceptance:** CRUD operations + `pollBackfillStatus()` returning `Flow<BackfillStatus>`; local cache in SQLDelight
  - **Files:** `shared/src/commonMain/kotlin/attendance/AbwesenheitRepository.kt`, `shared/src/commonMain/kotlin/attendance/AbwesenheitRepositoryImpl.kt`

- [ ] **T-022** SQLDelight schema + offline mutation queue
  - **Acceptance:** `attendance_responses`, `abwesenheit_rules`, `attendance_mutation_queue` tables in SQLDelight schema; queue entries: `(id, type, payload JSON, created_at)`; flush-on-reconnect logic; 409 reverts optimistic state + emits error event
  - **Files:** `shared/src/commonMain/sqldelight/attendance.sq`, `shared/src/commonMain/kotlin/attendance/MutationQueue.kt`

- [ ] **T-023** Optimistic UI updates
  - **Acceptance:** `PUT /events/{id}/attendance/me` updates local SQLDelight cache before API call; reverts to previous state on API error; error surfaced as `Flow` event (toast trigger)
  - **Files:** `shared/src/commonMain/kotlin/attendance/AttendanceRepositoryImpl.kt`

- [ ] **T-024** Deadline check in shared domain
  - **Acceptance:** Pure function `isDeadlinePassed(event: Event): Boolean` using `Clock.System.now()`; used in both UI (disable buttons) and repository (skip queue submission)
  - **Files:** `shared/src/commonMain/kotlin/attendance/DeadlineChecker.kt`

- [ ] **T-025** Client-side stats aggregation
  - **Acceptance:** Pure function `aggregateStats(rows: List<AttendanceRow>, filter: StatsFilter): AttendanceStats`; computes `presence_pct`, `training_pct`, `match_pct`; no network call; tested with unit tests
  - **Files:** `shared/src/commonMain/kotlin/attendance/StatsAggregator.kt`

---

## Phase 5 — Mobile UI (CMP)

- [ ] **T-026** S1 Event Detail — My Attendance card
  - **Acceptance:** Three inline tap targets (Confirmed / Declined / Unsure); optimistic update on tap; Confirmed taps sync immediately; Declined/Unsure open S2 sheet; buttons disabled + "Deadline reached" label after deadline; dual-role: horizontal scroll between summary card and My Attendance card
  - **Files:** `shared/src/commonMain/kotlin/ui/event/EventDetailScreen.kt`

- [ ] **T-027** S2 Begründung Sheet (bottom sheet)
  - **Acceptance:** Opens for Declined (optional) and Unsure (mandatory); inline validation blocks submit for Unsure without text; "Submit" triggers sync; dismissable for Declined
  - **Files:** `shared/src/commonMain/kotlin/ui/attendance/BegründungSheet.kt`

- [ ] **T-028** S3 Attendance List Screen
  - **Acceptance:** Section headers: Confirmed / Unsure / Declined / No Response with counts; expandable rows with Begründung + auto-decline indicator (⟳); coach rows tappable for inline override; status badges per spec
  - **Files:** `shared/src/commonMain/kotlin/ui/attendance/AttendanceListScreen.kt`

- [ ] **T-029** S6 My Absences Screen
  - **Acceptance:** Empty state with add button; list with type icon + label + date chip; swipe-left delete (confirm dialog); tap opens S7 sheet in edit mode
  - **Files:** `shared/src/commonMain/kotlin/ui/absences/MyAbsencesScreen.kt`

- [ ] **T-030** S7 Add/Edit Absence Sheet
  - **Acceptance:** Icon grid for 6 preset types; Recurring | Period toggle; Recurring: weekday multi-select grid + optional end date; Period: date range picker + optional custom label (pre-filled); "Save" creates/updates rule + shows backfill snackbar
  - **Files:** `shared/src/commonMain/kotlin/ui/absences/AbsenceSheet.kt`

- [ ] **T-031** S8 Player Statistics Screen
  - **Acceptance:** Filter bar (event type segmented + date range); progress rings/bars for `presence_pct`, `training_pct`, `match_pct`; stats computed from `StatsAggregator` using fetched raw rows
  - **Files:** `shared/src/commonMain/kotlin/ui/stats/PlayerStatsScreen.kt`

- [ ] **T-032** S9 Team Statistics Screen
  - **Acceptance:** Same filter bar; player list with avatar + name + presence % bar; tap → S8 (individual)
  - **Files:** `shared/src/commonMain/kotlin/ui/stats/TeamStatsScreen.kt`

- [ ] **T-033** Backfill snackbar
  - **Acceptance:** "Applying absence rules…" (persistent, no dismiss) while pending; auto-dismiss "Absence rules applied" (3s) on done; "Failed — tap to retry" (persistent, action) on failed; driven by `pollBackfillStatus()` flow
  - **Files:** `shared/src/commonMain/kotlin/ui/absences/AbsenceSheet.kt` or host scaffold

- [ ] **T-034** Status badge component
  - **Acceptance:** Variants: `confirmed` (green filled dot), `declined` (red filled dot), `unsure` (yellow filled dot), `declined-auto` (dashed outline + ⟳ icon); reusable composable
  - **Files:** `shared/src/commonMain/kotlin/ui/components/StatusBadge.kt`

- [ ] **T-035** Offline indicator
  - **Acceptance:** Queue badge / toast shown when mutations are queued; "Syncing…" toast on reconnect flush; reverts to normal after sync completes
  - **Files:** `shared/src/commonMain/kotlin/ui/components/OfflineIndicator.kt`
