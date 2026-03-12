---
template: handover
version: 0.1.0
status: DONE
---
# Handover: Event Scheduling

## What Was Built

Full event management backend + Android UI:
- One-off and recurring events (series with lazy materialisation, 12-month rolling window)
- Event types: `training`, `match`, `other`; optional meetup time
- Multi-team events: one event targets multiple teams; player sees deduplicated list with multi-team badge
- Sub-group audience restriction: event targets subset of team members
- Edit scopes for recurring: `this_only`, `this_and_future`, `all`
- Cancel with scope; duplicate event endpoint
- Daily materialisation job (also triggered on series creation)
- Calendar view + list view Android UI
- Sub-group management UI (create, edit members, delete)
- `kotlinx-datetime` for UTC storage + client-side timezone conversion

## Architecture Decisions

| Decision | Outcome |
|---|---|
| Recurring strategy | Lazy materialisation: series template stored in `event_series`; occurrences as concrete `events` rows with 12-month lookahead |
| Edit scopes | "This and future" = truncate series + new series; "All" = update template + re-apply to non-override occurrences |
| Multi-team dedup | Server groups by `event_id`, returns `matched_teams[]`; client renders multi-team indicator |
| Audience resolution | Membership gate: player must be in `event_teams` AND (no subgroup restriction OR in `event_subgroups`) |
| Stats | N/A — attendance stats owned by attendance-tracking feature |
| Time zones | All times stored UTC; `kotlinx-datetime` converts to device local in shared domain |

## Key Files

```
backend/src/main/kotlin/com/playbook/
  routes/EventRoutes.kt
  routes/SubgroupRoutes.kt
  jobs/EventMaterialisationJob.kt

shared/src/commonMain/.../domain/
  Event.kt, EventSeries.kt, Subgroup.kt

shared/src/commonMain/.../repository/
  EventRepository.kt, SubgroupRepository.kt

androidApp/src/.../ui/
  eventlist/         — player + coach list views
  eventcalendar/     — month/week calendar view
  eventdetail/       — detail + attendance summary
  eventform/         — create/edit (scope selector for recurring)
  subgroupmgmt/      — sub-group management
  components/EventTypeIndicator.kt   — shared indicator component
```

## Migrations

`backend/src/main/resources/db/migrations/` — event-scheduling migrations (V11–V15 range)
- Tables: `events`, `event_series`, `event_teams`, `event_subgroups`, `subgroups`, `subgroup_members`

## Known Limitations

- Materialisation job is in-process (Ktor coroutines) — not distributed; fine for MVP scale
- `this_and_future` edit creates a new series; historical link to original series is lost (by design)
- Past occurrences are immutable regardless of edit scope
- Web UI deferred (ADR-001)

## Downstream Dependencies

- Attendance tracking: creates `attendance_responses` per event member
- Notifications: consumes `event.created`, `event.updated`, `event.cancelled` hooks
