---
template: handover
version: 0.1.0
status: DONE (2 manual Xcode steps pending)
---
# Handover: Notifications

## What Was Built

Full push + in-app notification system:
- In-app inbox: paginated, read/unread, dismiss; SQLDelight local cache; unread count from cache
- Notification settings: per-type toggle + timing preferences; row auto-created on first login
- Push tokens: multi-device support; upserted on app launch; deregistered on logout
- Push provider: **OneSignal** behind `PushService` interface (swappable)
- Trigger hooks: event created/edited/cancelled, attendance response, abwesenheit change
- Scheduled jobs: reminder (every 5 min, checks `start_time - lead_time`) and pre-event summary (coaches)
- Dedup table: SHA-256 key prevents duplicate delivery; TTL 7 days
- Fan-out strategy: in-process for small teams; background queue for large teams
- `expect/actual`: push permission request, SDK init, deep link handling — per platform
- Android: bottom nav badge, notification list screen, settings screen; full push wiring
- iOS: 2 manual Xcode steps documented in `iosApp/README.md` (NT-011, NT-016)

## Architecture Decisions

| Decision | Outcome |
|---|---|
| Push provider | OneSignal; abstracted behind `PushService` — swap without touching trigger logic |
| External user ID | Our `user_id` = OneSignal external user ID (set on login via `OneSignal.login(userId)`) |
| In-app inbox | Reads from SQLDelight cache; background sync on open |
| Unread count | Derived from local cache — no separate API call |
| Trigger dispatch | Background jobs off request path — never blocks API response |
| Dedup | `notification_dedup` table with `sha256(user_id + type + reference_id + time_bucket)` key |
| Scheduler cadence | 5-minute polling loop; covers reminder + summary triggers |

## Key Files

```
backend/src/main/kotlin/com/playbook/
  routes/NotificationRoutes.kt
  routes/NotificationSettingsRoutes.kt
  routes/PushTokenRoutes.kt
  jobs/ReminderSchedulerJob.kt
  jobs/AttendanceSummaryJob.kt
  push/PushService.kt          — interface
  push/OneSignalPushService.kt — active implementation

shared/src/commonMain/.../domain/
  Notification.kt, NotificationSettings.kt

shared/src/commonMain/.../repository/
  NotificationRepository.kt
  NotificationSettingsRepository.kt

shared/src/commonMain/.../db/
  Notification.sq   — SQLDelight inbox cache

androidApp/src/.../ui/
  notifications/    — inbox list screen
  settings/notifications/   — notification settings screen

iosApp/README.md   — documents NT-011, NT-016 manual Xcode steps
```

## Migrations

`backend/src/main/resources/db/migrations/` — notifications migrations (V22+ range)
- Tables: `notifications`, `notification_settings`, `push_tokens`, `notification_dedup`

## Remaining Manual Steps

See `iosApp/README.md`:
- **NT-011**: Add Push Notification capability in Xcode project settings
- **NT-016**: Configure OneSignal App ID in Xcode scheme environment variables

## Known Limitations

- Reminder/summary scheduler uses polling (5-min loop); at scale, a proper job queue (e.g. Redis + worker) would be better
- Dedup table grows indefinitely until scheduled TTL cleanup runs — no cleanup verified in MVP
- Web push is OneSignal best-effort; not tested (ADR-001 web deferred)
- OneSignal is a third-party dependency; if it becomes unavailable, swap `PushService` implementation

## Upstream Dependencies

- All other features: trigger notification hooks
- event-scheduling: event mutation hooks
- attendance-tracking: response + abwesenheit hooks
