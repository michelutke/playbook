---
template: plan
version: 0.1.0
gate: READY
---
# Implementation Plan: Event Scheduling

> **Depends on:** team-management (teams, memberships, subgroups referenced here)
> **Implement second** — events are the anchor for attendance tracking and notifications.

---

## Phase 1 — Database Schema

- [ ] Create `event_series` table (id, pattern_type, weekdays, interval_days, series_start_date, series_end_date, template_start_time, template_end_time, created_by, created_at)
- [ ] Create `events` table (id, title, type, start_at, end_at, meetup_at, location, description, min_attendees, status, cancelled_at, series_id FK, series_sequence, series_override, created_by, timestamps)
- [ ] Create `event_teams` join table (event_id, team_id) — PK `(event_id, team_id)`
- [ ] Create `event_subgroups` join table (event_id, subgroup_id) — PK `(event_id, subgroup_id)`
- [ ] Create `subgroups` table (id, team_id, name, created_at)
- [ ] Create `subgroup_members` table (subgroup_id, user_id) — PK `(subgroup_id, user_id)`
- [ ] Add indexes: `events(series_id, series_sequence)`, `events(start_at, status)`, `event_teams(team_id, event_id)`, `event_subgroups(event_id)`, `subgroup_members(user_id)`

## Phase 2 — Ktor Backend

### Audience resolution helper
- [ ] Implement `resolveTargetedUsers(eventId)` — joins `event_teams` + `event_subgroups` + `subgroup_members`; returns distinct user IDs eligible for this event

### Event endpoints
- [ ] `GET /users/me/events` — deduplicated list with `matched_teams[]`; params: `from`, `to`, `type`, `teamId`
- [ ] `GET /teams/{teamId}/events` — team-scoped list (coach view)
- [ ] `GET /events/{id}` — event detail
- [ ] `POST /events` — create one-off or series (creates `event_series` + materialises first occurrence)
- [ ] `PATCH /events/{id}` — edit with `scope: this_only | this_and_future | all`
  - `this_only`: update row, set `series_override = true`
  - `this_and_future`: close series at `sequence - 1`, create new series + rematerialise
  - `all`: update `event_series` + re-apply to future non-override occurrences
- [ ] `POST /events/{id}/cancel` — cancel with scope; preserve attendance; set `cancelled_at`
- [ ] `POST /events/{id}/duplicate` — clone into new create payload

### Sub-group endpoints
- [ ] `GET /teams/{teamId}/subgroups` — list with member counts
- [ ] `POST /teams/{teamId}/subgroups` — create
- [ ] `PATCH /subgroups/{id}` — update name + members
- [ ] `DELETE /subgroups/{id}` — delete; remove from `event_subgroups` (events go team-wide)

### Notification triggers
- [ ] Emit `event.created`, `event.updated`, `event.cancelled` domain events (consumed by notifications module)

## Phase 3 — Materialisation Background Job

- [ ] Implement `materializeSeries(seriesId, fromDate, toDate)` — generates `events` rows from `event_series` pattern; idempotent (upsert by `series_id + series_sequence`)
- [ ] On series creation: immediately materialise next 12 months
- [ ] Daily coroutine job: for each active series, ensure occurrences materialised ≥ 12 months ahead
- [ ] `events(series_override = false)` only updated on "all" edits; `series_override = true` rows untouched

## Phase 4 — KMP Domain Layer

- [ ] Define `EventRepository` interface + impl (fetch, create, edit, cancel, duplicate)
- [ ] Define `SubgroupRepository` interface + impl
- [ ] Timezone conversion utility: `Instant.toLocalDateTime(TimeZone.currentSystemDefault())` — shared domain, not in Composables
- [ ] Multi-team deduplication: group events by `event_id`, merge `matched_teams[]` — in shared domain layer
- [ ] `kotlinx-datetime 0.7.1`: use `kotlin.time.Instant` (not `kotlinx.datetime.Instant` — removed in 0.7.x)

## Phase 5 — Mobile UI (CMP)

- [ ] **S1 Event List** — filter chips (team + type); list items with type icon + status badge; coach FAB
- [ ] **S2 Calendar View**
  - Month: `kizitonwose/Calendar compose-multiplatform 2.9.0`; dot indicators per event type; tap day → inline list
  - Week: custom time-block grid (`LazyColumn` of hour slots + event block overlays)
- [ ] **S3 Event Detail** — header, location ("Open in Maps"), team/subgroup tag, multi-team badge, attendance summary card, coach `⋯` menu
- [ ] **S4 Create/Edit Event Form** — sectioned scroll: Basic, Time, Location, Audience, Options, Recurring toggle
- [ ] **S5 Recurring Pattern Sheet** — Daily / Weekly / Custom; weekday grid; optional end date
- [ ] **S6 Edit Recurring Scope Sheet** — `this_only` / `this_and_future` / `all`
- [ ] **S7 Sub-group Management Screen** — list; tap to edit; swipe delete with confirmation
- [ ] Recurring indicator (⟳ icon) on list items and calendar blocks
- [ ] Cancelled state: greyed row + "Cancelled" chip + strikethrough in calendar
- [ ] Multi-day event display: date range label in list; spanning blocks in week calendar
