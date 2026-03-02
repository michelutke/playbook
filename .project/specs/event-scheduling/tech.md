---
template: tech
version: 0.1.0
gate: READY
---
# Tech Spec: Event Scheduling

## Platform Scope

- Mobile (iOS + Android): KMP shared domain/data + CMP UI
- Web: deferred per ADR-001
- All times stored UTC; displayed in user's local timezone (client-side conversion)

---

## Data Model

### `events`
| Column | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `title` | TEXT NOT NULL | |
| `type` | ENUM(`training`, `match`, `other`) | |
| `start_at` | TIMESTAMPTZ | UTC |
| `end_at` | TIMESTAMPTZ | UTC; may be different day (multi-day) |
| `meetup_at` | TIMESTAMPTZ NULLABLE | optional pre-event meetup time |
| `location` | TEXT NULLABLE | |
| `description` | TEXT NULLABLE | |
| `min_attendees` | INT NULLABLE | |
| `status` | ENUM(`active`, `cancelled`) | default `active` |
| `cancelled_at` | TIMESTAMPTZ NULLABLE | |
| `series_id` | UUID FK → event_series NULLABLE | null if one-off |
| `series_sequence` | INT NULLABLE | ordinal position in series |
| `series_override` | BOOLEAN | true if this occurrence was individually edited |
| `created_by` | UUID FK → users | |
| `created_at` | TIMESTAMPTZ | |
| `updated_at` | TIMESTAMPTZ | |

### `event_series` (recurring pattern template)
| Column | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `pattern_type` | ENUM(`daily`, `weekly`, `custom`) | |
| `weekdays` | SMALLINT[] NULLABLE | `0`=Mon…`6`=Sun; for `weekly` |
| `interval_days` | INT NULLABLE | for `custom` |
| `series_start_date` | DATE | first occurrence date |
| `series_end_date` | DATE NULLABLE | null = indefinite |
| `template_start_time` | TIME | used when materialising new occurrences |
| `template_end_time` | TIME | |
| `created_by` | UUID FK → users | |
| `created_at` | TIMESTAMPTZ | |

### `event_teams` (events ↔ teams, many-to-many)
| Column | Type | Notes |
|---|---|---|
| `event_id` | UUID FK → events | |
| `team_id` | UUID FK → teams | |

PK: `(event_id, team_id)`

### `event_subgroups` (optional audience restriction)
| Column | Type | Notes |
|---|---|---|
| `event_id` | UUID FK → events | |
| `subgroup_id` | UUID FK → subgroups | |

PK: `(event_id, subgroup_id)`. No rows = whole team targeted.

### `subgroups`
| Column | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `team_id` | UUID FK → teams | |
| `name` | TEXT NOT NULL | |
| `created_at` | TIMESTAMPTZ | |

### `subgroup_members`
| Column | Type | Notes |
|---|---|---|
| `subgroup_id` | UUID FK → subgroups | |
| `user_id` | UUID FK → users | |

PK: `(subgroup_id, user_id)`

---

## Recurring Event Strategy

**Approach: lazy materialisation with exceptions.**

- Series pattern stored in `event_series`
- Occurrences materialised as concrete `events` rows by a background job (rolling **12-month window** — covers a full sports season)
- Individually modified occurrences flagged with `series_override = true`

### Edit scopes (FR-ES-09)

| Scope | Action |
|---|---|
| **This event only** | Update single `events` row; set `series_override = true` |
| **This and future** | Set `series_end_date` to `series_sequence - 1` on current series; create new `event_series` + materialise future occurrences with new properties |
| **All** | Update `event_series` template; re-apply to all future materialised occurrences where `series_override = false` |

Past occurrences are never modified regardless of scope.

### Materialisation job
- Runs daily; ensures every series has materialised occurrences ≥ 12 months ahead
- Also triggered immediately on series creation
- Occurrences that have been individually cancelled/edited are skipped

### Sub-group deleted (edge case)
- `event_subgroups` rows removed → events become team-wide automatically

---

## Multi-team Events

A player in multiple teams targeted by the same event sees **one event entry** in their list and calendar. The entry is visually highlighted with a multi-team badge (e.g. "2 teams") and the detail screen lists which teams are involved.

Server deduplicates: event list query groups by `event_id`, returns `matched_teams[]` per event. Client renders the multi-team indicator when `matched_teams.length > 1`.

---

## API — Ktor Backend

### Events
| Method | Path | Description |
|---|---|---|
| GET | `/users/me/events` | Player's deduplicated event list; params: `from`, `to`, `type`, `teamId` |
| GET | `/teams/{teamId}/events` | Team-scoped event list (coach view) |
| GET | `/events/{id}` | Event detail |
| POST | `/events` | Create event (one-off or series) |
| PATCH | `/events/{id}` | Edit; body includes `scope` for recurring |
| POST | `/events/{id}/cancel` | Cancel; body includes `scope` |
| POST | `/events/{id}/duplicate` | Pre-fill new create from existing |

`PATCH` / cancel body for recurring:
```json
{ "scope": "this_only | this_and_future | all", ...changes }
```

`/users/me/events` response includes `matched_teams: [{ id, name }]` per event.

### Sub-groups
| Method | Path | Description |
|---|---|---|
| GET | `/teams/{teamId}/subgroups` | List subgroups + member counts |
| POST | `/teams/{teamId}/subgroups` | Create subgroup |
| PATCH | `/subgroups/{id}` | Update name / members |
| DELETE | `/subgroups/{id}` | Delete; cascades audience to team-wide |

---

## Targeted Audience Resolution

When fetching events for a player, server filters by:
1. Player is member of a team the event targets (`event_teams`)
2. AND either: no subgroup restriction OR player is in one of the targeted subgroups (`event_subgroups`)

This determines which players receive attendance requests and notifications.

---

## Indexes

- `events(series_id, series_sequence)` — scope-based edits
- `events(start_at, status)` — date-range listing
- `event_teams(team_id, event_id)` — team event queries
- `event_subgroups(event_id)` — audience resolution
- `subgroup_members(user_id)` — player subgroup lookups

---

## KMP Architecture

- `EventRepository`, `SubgroupRepository` in shared KMP domain module
- Timezone conversion in shared domain via `kotlinx-datetime`; UTC stored, local displayed
- Calendar view state (month/week) in shared ViewModel
- Multi-team deduplication logic in shared domain — not in Composables

---

## Notifications

Event mutations (create / edit / cancel) trigger player notifications. Dispatch owned by the **notifications** feature; event-scheduling emits domain events consumed by that module.
