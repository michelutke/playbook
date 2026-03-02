---
template: tasks
version: 0.1.0
gate: READY GO
---
# Tasks: Event Scheduling

> **Depends on:** team-management (teams, memberships, subgroups). Implement second.

## Phase 1 — Database Schema

| ID | Task | Deps |
|---|---|---|
| ES-001 | Create `event_series` table — pattern_type, weekdays, interval_days, series_start/end_date, template_start/end_time, created_by | — |
| ES-002 | Create `events` table — title, type, start_at, end_at, meetup_at, location, description, min_attendees, status, cancelled_at, series_id FK, series_sequence, series_override, created_by | ES-001 |
| ES-003 | Create `event_teams` join table — PK `(event_id, team_id)` | ES-002 |
| ES-004 | Create `event_subgroups` join table — PK `(event_id, subgroup_id)` | ES-002 |
| ES-005 | Create `subgroups` table — id, team_id FK, name, created_at | — |
| ES-006 | Create `subgroup_members` table — PK `(subgroup_id, user_id)` | ES-005 |
| ES-007 | Add indexes: `events(series_id, series_sequence)`, `events(start_at, status)`, `event_teams(team_id, event_id)`, `event_subgroups(event_id)`, `subgroup_members(user_id)` | ES-002, ES-003, ES-004, ES-006 |

## Phase 2 — Ktor Backend

### Audience Helper
| ID | Task | Deps |
|---|---|---|
| ES-008 | `resolveTargetedUsers(eventId)` — joins event_teams + event_subgroups + subgroup_members; returns distinct user IDs; no subgroup rows = whole team | ES-003, ES-004, ES-006 |

### Event Endpoints
| ID | Task | Deps |
|---|---|---|
| ES-009 | `GET /users/me/events` — deduplicated list; groups by event_id; returns `matched_teams[]`; params: `from`, `to`, `type`, `teamId` | ES-003 |
| ES-010 | `GET /teams/{teamId}/events` — team-scoped list (coach view) | ES-003 |
| ES-011 | `GET /events/{id}` — event detail with teams + subgroups | ES-002 |
| ES-012 | `POST /events` — create one-off or series; creates event_series row if recurring; triggers immediate materialisation | ES-002, ES-003 |
| ES-013 | `PATCH /events/{id}` — edit with `scope: this_only \| this_and_future \| all`; this_only sets series_override; this_and_future closes series + creates new; all re-applies to non-override future rows | ES-002, ES-012 |
| ES-014 | `POST /events/{id}/cancel` — cancel with scope; preserves attendance records; sets cancelled_at | ES-013 |
| ES-015 | `POST /events/{id}/duplicate` — returns pre-filled create payload; does not persist | ES-011 |

### Sub-group Endpoints
| ID | Task | Deps |
|---|---|---|
| ES-016 | `GET /teams/{teamId}/subgroups` — list with member counts | ES-005, ES-006 |
| ES-017 | `POST /teams/{teamId}/subgroups` — create subgroup | ES-005 |
| ES-018 | `PATCH /subgroups/{id}` — update name + members | ES-006 |
| ES-019 | `DELETE /subgroups/{id}` — delete; remove from event_subgroups (events become team-wide) | ES-004, ES-006 |

### Notification Triggers
| ID | Task | Deps |
|---|---|---|
| ES-020 | Emit `event.created`, `event.updated`, `event.cancelled` domain events consumed by notifications module | ES-012, ES-013, ES-014 |

## Phase 3 — Materialisation Background Job

| ID | Task | Deps |
|---|---|---|
| ES-021 | `materializeSeries(seriesId, fromDate, toDate)` — generates events rows from pattern; upsert by `(series_id, series_sequence)` (idempotent) | ES-001, ES-002 |
| ES-022 | On series creation: immediately materialise 12 months of occurrences | ES-021, ES-012 |
| ES-023 | Daily coroutine job: for each active series ensure occurrences materialised ≥ 12 months ahead; skip series_override rows on "all" edits | ES-021 |

## Phase 4 — KMP Domain Layer

| ID | Task | Deps |
|---|---|---|
| ES-024 | `EventRepository` interface + impl — fetch list, fetch detail, create, edit (scope), cancel (scope), duplicate | ES-009, ES-010, ES-011, ES-012, ES-013, ES-014, ES-015 |
| ES-025 | `SubgroupRepository` interface + impl — list, create, update, delete | ES-016, ES-017, ES-018, ES-019 |
| ES-026 | Timezone conversion utility in shared domain — `Instant.toLocalDateTime(TimeZone.currentSystemDefault())`; use `kotlin.time.Instant` (not removed `kotlinx.datetime.Instant`) | — |
| ES-027 | Multi-team deduplication in shared domain — group events by `event_id`; merge `matched_teams[]`; logic not in Composables | ES-024 |

## Phase 5 — Mobile UI (CMP)

| ID | Task | Deps |
|---|---|---|
| ES-028 | **S1 Event List** — filter chips (team + type); list items with type icon + status badge + ⟳ recurring indicator; coach FAB; cancelled state (greyed + chip) | ES-024 |
| ES-029 | **S2 Calendar — Month View** — kizitonwose `compose-multiplatform 2.9.0`; dot indicators per event type; tap day → inline list | ES-024 |
| ES-030 | **S2 Calendar — Week View** — custom `LazyColumn` of hour slots + event block overlays; multi-day spanning blocks | ES-024 |
| ES-031 | **S3 Event Detail** — header, location with "Open in Maps", team/subgroup tags, multi-team badge, attendance summary card, coach ⋯ menu | ES-024, ES-027 |
| ES-032 | **S4 Create/Edit Event Form** — sectioned scroll: Basic, Time, Location, Audience (teams + subgroups), Options, Recurring toggle | ES-024, ES-025 |
| ES-033 | **S5 Recurring Pattern Sheet** — Daily / Weekly / Custom selector; weekday grid (Mon–Sun); optional end date | ES-032 |
| ES-034 | **S6 Edit Recurring Scope Sheet** — `this_only` / `this_and_future` / `all` options with consequence descriptions | ES-032 |
| ES-035 | **S7 Sub-group Management Screen** — list with member counts; tap to edit; swipe delete with confirmation | ES-025 |
| ES-036 | Polish: recurring ⟳ icon on list items + calendar blocks; cancelled strikethrough in calendar; multi-day date range label | ES-028, ES-029, ES-030 |
