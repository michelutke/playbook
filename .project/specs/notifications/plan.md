---
template: plan
version: 0.1.0
gate: READY
---
# Implementation Plan: Notifications

> **Depends on:** team-management, event-scheduling, attendance-tracking
> **Implement fourth** — consumes domain events emitted by all other features.

---

## Phase 1 — Database Schema

- [ ] Create `notifications` table (id, user_id, type ENUM, title, body, deep_link, reference_id, read, created_at)
- [ ] Create `notification_settings` table (user_id PK, new_events, event_changes, event_cancellations, reminders, reminder_lead_time, attendance_per_response, attendance_summary, attendance_summary_lead_time, abwesenheit_changes) — auto-created with defaults on first login
- [ ] Create `push_tokens` table (id, user_id, platform ENUM, token) — unique `(user_id, token)`
- [ ] Create `notification_dedup` table (key TEXT PK, created_at) — TTL 7 days
- [ ] Add indexes: `notifications(user_id, created_at DESC)`, `notifications(user_id, read)`, `push_tokens(user_id)`

## Phase 2 — OneSignal SDK Integration

### Android (androidMain)
- [ ] Add OneSignal Android SDK 5.6.1+ to Android Gradle config
- [ ] `initPushSdk()` actual: `OneSignal.initWithContext(context, appId)` in `Application.onCreate`
- [ ] `OneSignal.login(userId)` on user auth; `OneSignal.logout()` on sign-out
- [ ] `requestPushPermission()` actual: `OneSignal.Notifications.requestPermission(fallbackToSettings, activity)`
- [ ] Store `OneSignal.User.pushSubscription.id` → `POST /push-tokens` on launch

### iOS (iosMain)
- [ ] Add OneSignal XCFramework 5.x via Swift Package Manager
- [ ] Create Swift shim `OneSignalWrapper.swift` (ObjC-bridged) for KMP interop
- [ ] `initPushSdk()` actual: call shim → `OneSignal.initialize(appId, launchOptions)`
- [ ] `OneSignal.login(externalId)` via shim on auth
- [ ] `requestPushPermission()` actual: `OneSignal.Notifications.requestPermission(callback)`
- [ ] Enable "Background Modes → Remote notifications" capability in Xcode

### KMP expect/actual
- [ ] Define `expect fun initPushSdk()`, `expect fun requestPushPermission(): Flow<PermissionState>`, `expect fun handleDeepLink(link: String)` in `commonMain`
- [ ] Implement `actual` in `androidMain` and `iosMain`

## Phase 3 — Ktor Backend

### Notification inbox endpoints
- [ ] `GET /notifications` — paginated inbox, newest first
- [ ] `PUT /notifications/{id}/read` — mark single read
- [ ] `PUT /notifications/read-all` — mark all read
- [ ] `DELETE /notifications/{id}` — dismiss

### Notification settings endpoints
- [ ] `GET /notifications/settings` — own settings
- [ ] `PUT /notifications/settings` — partial update; upsert row with defaults if not exists

### Push token endpoints
- [ ] `POST /push-tokens` — register `{ platform, token }`; upsert `(user_id, token)`
- [ ] `DELETE /push-tokens/{token}` — deregister on logout

### PushService abstraction
- [ ] Define `PushService` interface: `suspend fun send(userIds, title, body, deepLink, data)`
- [ ] Implement `OneSignalPushService`: Ktor HTTP client → `POST https://api.onesignal.com/notifications` with `include_aliases.external_id`; chunk at 2,000 per request
- [ ] Register `PushService` as Koin singleton (injectable in trigger jobs)

### Fan-out helper
- [ ] `resolveRecipients(eventId, notificationType)`: query targeted users + filter by membership + filter by notification setting
- [ ] Write dedup key before sending: `sha256(userId + type + referenceId + timeBucket)`; skip if key exists

## Phase 4 — Trigger Hooks & Schedulers

### Domain event triggers
- [ ] `event.created` hook → notify all targeted players (`new_events` setting gate)
- [ ] `event.updated` hook → notify all targeted players if time/location changed (`event_changes`)
- [ ] `event.cancelled` hook → notify all targeted players; cancel pending reminder entries (`event_cancellations`)
- [ ] `attendance_response.upserted` hook → notify event's coaches (`attendance_per_response`)
- [ ] `abwesenheit_rule.upserted` hook → notify team's coaches (`abwesenheit_changes`)
- [ ] All triggers run as background coroutines (non-blocking off request path)

### Reminder scheduler (every 5 min)
- [ ] Coroutine: `while (isActive) { checkReminders(); delay(5.minutes) }`
- [ ] Query: events with `start_at` in upcoming window matching `reminder_lead_time` options
- [ ] Per user + event: check dedup table; if not exists → send push + write dedup key
- [ ] Skip if event `cancelled` or user no longer active team member

### Pre-event summary scheduler (every 5 min, same loop)
- [ ] For each coach with `attendance_summary = true`: fire at `start_at - attendance_summary_lead_time`
- [ ] Payload: count of `no-response` players at send time
- [ ] Dedup same way as reminders

### Settings auto-create
- [ ] On first user login: `INSERT INTO notification_settings (user_id) VALUES (?) ON CONFLICT DO NOTHING`

## Phase 5 — KMP Domain Layer

- [ ] Define `NotificationRepository` interface + impl
- [ ] Define `NotificationSettingsRepository` interface + impl
- [ ] **SQLDelight local cache** for inbox: mirror `notifications` table locally; reads from cache; sync on inbox open
- [ ] Unread count: derived from local cache (`SELECT COUNT(*) WHERE read = 0`); no extra API call
- [ ] `initPushSdk()` called from app startup sequence (before user session established)
- [ ] Deep link routing: `handleDeepLink("/events/{id}")` → navigate to Event Detail

## Phase 6 — Mobile UI (CMP)

- [ ] **Notification inbox screen** — paginated list; unread badge; mark-read on tap; swipe-dismiss
- [ ] **Notification settings screen** — toggle per type; lead time picker for reminders + summaries
- [ ] **Push permission prompt** — shown on first launch after onboarding; if denied → persistent banner per ux-patterns
- [ ] **Unread badge** on inbox nav icon (derived from local cache count)
- [ ] **Deep link handling**: app handles `teamorg://events/{id}`, `teamorg://teams/{id}` etc. → correct screen
- [ ] In-app notification fallback: if push denied, notifications visible in inbox only; warning snackbar shown once
