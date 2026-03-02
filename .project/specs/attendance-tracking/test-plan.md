---
template: test-plan
version: 0.1.0
---
# Test Plan: Attendance Tracking

---

## Unit Tests

### Backend (Kotlin/JUnit)

| Test | Task | Description |
|---|---|---|
| `PUT /attendance/me — unsure without reason returns 400` | T-007 | Service rejects unsure response with no reason field |
| `PUT /attendance/me — after deadline returns 409` | T-007 | Service rejects submission when `Clock.now() > event.responseDeadline` |
| `PUT /attendance/me — sets manual_override when rule present` | T-007 | `manual_override=true` when active `abwesenheit_rule_id` on existing row |
| `backfillAbwesenheit — skips manual_override rows` | T-017 | Job does not overwrite rows with `manual_override=true` |
| `backfillAbwesenheit — matches recurring rule weekdays` | T-017 | Events on matching weekdays upserted; others skipped |
| `backfillAbwesenheit — matches period rule date range` | T-017 | Events within `[start_date, end_date]` upserted |
| `autoPresentJob — sets present for confirmed on check_in_disabled events` | T-018 | Only `confirmed` responses → `present`; others untouched |
| `autoPresentJob — no-op for check_in_enabled events` | T-018 | No `attendance_records` inserted when `check_in_enabled=true` |
| `DELETE abwesenheit — existing declined-auto unchanged` | T-011 | Past `declined-auto` rows persist after rule deletion |

### KMP Shared Domain (Kotlin/KMP)

| Test | Task | Description |
|---|---|---|
| `isDeadlinePassed — returns true after deadline` | T-024 | `Clock.System.now() > deadline` → true |
| `isDeadlinePassed — returns false before deadline` | T-024 | False when deadline in future |
| `aggregateStats — presence_pct correct` | T-025 | `present` / total events = correct % |
| `aggregateStats — training_pct filters by event type` | T-025 | Only training events counted for training % |
| `aggregateStats — match_pct filters by event type` | T-025 | Only match events counted for match % |
| `aggregateStats — empty rows returns zeros` | T-025 | No divide-by-zero; returns 0% for empty input |
| `mutationQueue — flushes in order on reconnect` | T-022 | Mutations applied FIFO |
| `mutationQueue — 409 reverts optimistic state` | T-022 | Local cache reverts; error event emitted |

---

## Integration Tests

### Backend API (Ktor test client)

| Test | Task | Description |
|---|---|---|
| `GET /events/{id}/attendance — returns grouped responses` | T-005 | Response grouped by status with correct counts |
| `GET /events/{id}/attendance/me — no-response default` | T-006 | Returns `no-response` when no row exists |
| `PUT /events/{id}/attendance/me — happy path confirmed` | T-007 | Row upserted; `manual_override=false` |
| `GET /events/{id}/check-in — 403 for non-coach` | T-013 | Player role returns 403 |
| `PUT /events/{id}/check-in/{userId} — captures previous state` | T-014 | `previous_status` + `previous_set_by` populated on update |
| `POST /users/me/abwesenheit — enqueues backfill job` | T-009 | Job row created with `pending` status; `job_id` returned |
| `GET /users/me/abwesenheit/backfill-status — returns status` | T-012 | Returns `pending | done | failed` for latest job |
| `GET /users/{userId}/attendance — respects date range` | T-015 | Only rows within `from`/`to` returned |
| `GET /teams/{teamId}/attendance — 403 for non-coach` | T-016 | Player accessing team-wide data returns 403 |
| `Backfill job — end-to-end rule creation → declined-auto inserted` | T-017 | Rule created → job runs → matching events have `declined-auto` rows |
| `Auto-present job — end-to-end event ends → present inserted` | T-018 | Event ends with `check_in_enabled=false` → all confirmed get present record |
| `Event creation trigger — abwesenheit entries created` | T-019 | New event creation generates `declined-auto` for matching rules |

---

## Manual / E2E Tests

| Scenario | Screens | Steps |
|---|---|---|
| Player confirms attendance | S1 | Tap Confirmed → optimistic update → sync; status badge shows green |
| Player declines with reason | S1, S2 | Tap Declined → sheet opens → enter reason → Submit → sync |
| Player responds as Unsure without reason | S2 | Tap Unsure → sheet → Submit without text → inline error shown; not submitted |
| Deadline enforcement | S1 | Open event after deadline → buttons disabled; "Deadline reached" shown |
| Coach overrides attendance | S3 | Coach taps player row → toggle Present/Absent/Excused → saved |
| Abwesenheit rule creation (Recurring) | S6, S7 | Add absence → pick preset → Recurring → select weekdays → Save → snackbar appears |
| Abwesenheit rule creation (Period) | S6, S7 | Add absence → pick preset → Period → select date range → Save → snackbar appears |
| Backfill snackbar lifecycle | S6, S7 | Create rule → "Applying…" snackbar → polls → "Applied" (3s) → gone |
| Backfill failure retry | S6, S7 | Simulate failed job → "Failed — tap to retry" persistent snackbar → tap retries |
| Swipe delete absence | S6 | Swipe left → confirm dialog → deleted; past declined-auto events unchanged |
| Player statistics filter | S8 | Select event type "Match" + date range → stats update showing only match data |
| Team statistics drill-down | S9 | Tap player row → navigates to S8 for that player |
| Offline response | S1 | Go offline → tap Confirmed → optimistic update → reconnect → sync → server confirmed |
| Offline 409 revert | S1 | Queue mutation → server returns 409 → local state reverts → toast shown |
| Auto-present (check-in disabled) | — | Event ends → verify attendance_records show `present` for confirmed players |
| Auto-present (check-in enabled) | S3 | Event ends → coach submits attendance list → locked |

---

## Acceptance Criteria Coverage

| AC | Tasks | Test Type |
|---|---|---|
| `unsure` requires reason (400) | T-007 | Unit + Integration |
| Late submission rejected (409) | T-007 | Unit + Integration |
| `manual_override=true` blocks backfill | T-007, T-017 | Unit + Integration |
| Backfill runs non-blocking | T-017 | Integration |
| Auto-present only for `check_in_disabled` | T-018 | Unit + Integration |
| Rule delete preserves past entries | T-011 | Unit + Integration |
| Deadline enforced client-side | T-024 | Unit + Manual |
| Optimistic update reverts on failure | T-023 | Unit + Manual |
| Stats computed client-side (no endpoint) | T-025 | Unit + Manual |
| Offline mutations queue and flush | T-022 | Unit + Manual |
| Status badges render correct variants | T-034 | Manual |
| Backfill snackbar lifecycle | T-033 | Manual |
