---
template: test-plan
version: 0.1.0
---
# Test Plan: Event Scheduling

## Unit Tests

### Backend

| # | Subject | Cases |
|---|---|---|
| U-01 | `materializeSeries()` | daily pattern generates correct dates; weekly pattern respects `weekdays[]`; honours `series_end_date`; second call is idempotent (no duplicate rows) |
| U-02 | PATCH scope logic | `this_only` sets `series_override = true`; `this_and_future` closes series at correct sequence + creates new series; `all` skips rows where `series_override = true` |
| U-03 | `resolveTargetedUsers()` | team-only event returns all active team members; subgroup-scoped event returns only subgroup members; multi-team event merges distinct users; user in two subgroups appears once |
| U-04 | Cancel scope logic | `this_only` sets `cancelled_at` on single row; `all` cancels all future non-cancelled occurrences; past events unchanged regardless of scope |
| U-05 | Series end-date boundary | event on `series_end_date` materialised; event one day after excluded |

### KMP Domain

| # | Subject | Cases |
|---|---|---|
| U-06 | Multi-team deduplication | same `event_id` from two teams → single row; `matched_teams` contains both; no dedup needed for single team |
| U-07 | Timezone conversion utility | UTC `Instant` → correct local `LocalDateTime` for a known Swiss timezone offset |

---

## Integration Tests

| # | Flow | Assertions |
|---|---|---|
| I-01 | Create one-off event | `events` row inserted; `event_teams` row inserted; `event_series` null; 201 response |
| I-02 | Create recurring series | `event_series` row created; 12 months of `events` materialised; all rows have correct `series_id` + sequential `series_sequence` |
| I-03 | Edit `this_only` | `series_override = true` on target row; sibling occurrences unchanged |
| I-04 | Edit `this_and_future` | original series `series_end_date` set to `sequence - 1`; new `event_series` created; future occurrences point to new series |
| I-05 | Edit `all` | `event_series` template updated; future `series_override = false` rows re-applied; `series_override = true` rows untouched; past rows unchanged |
| I-06 | Cancel `all` | all future non-cancelled occurrences get `cancelled_at`; past and already-cancelled rows unchanged |
| I-07 | Delete subgroup | `event_subgroups` rows removed; `resolveTargetedUsers` returns full team for affected events |
| I-08 | Duplicate event | returns 200 with pre-filled payload; no new row persisted |
| I-09 | Multi-team player event list | player in 2 targeted teams → deduplicated entry with `matched_teams.length == 2` |
| I-10 | Daily materialisation job | after job runs, series missing occurrences in 12-month window gets new materialised rows; existing rows unchanged |

---

## Edge Case Tests

| # | Scenario | Expected |
|---|---|---|
| E-01 | Series with `series_end_date` in the past | materialisation produces zero new rows |
| E-02 | Materialise same series twice | idempotent upsert — no duplicate `events` rows |
| E-03 | Subgroup with zero members targeted | `resolveTargetedUsers` returns empty set; no attendance requests generated |
| E-04 | Multi-day event (`end_at` on different calendar day) | `GET /users/me/events` returns single entry; week calendar block spans correct hours |
| E-05 | Edit `all` on mix of override + non-override occurrences | non-override rows updated; override rows preserved exactly |

---

## Manual / E2E Tests

| # | Journey | Steps |
|---|---|---|
| M-01 | Create weekly recurring event | Create event → toggle Recurring → Weekly → pick Mon + Wed → Save → calendar shows dots on Mon + Wed for 12 months |
| M-02 | Edit single occurrence | Tap event in series → Edit → Change title → "This event only" → confirm → that occurrence shows new title; ⟳ icon remains; other occurrences unchanged |
| M-03 | Cancel series from future | Cancel event → "This and future" → past events still visible; future events show cancelled state (greyed + chip) |
| M-04 | Sub-group scoped event | Create subgroup with 2 of 5 players → create event targeting subgroup → only 2 players see event in their list |
| M-05 | Week calendar time-block grid | Create 2 events overlapping same hour → both blocks visible in week view; correct start/end positions; multi-day event spans correctly |
