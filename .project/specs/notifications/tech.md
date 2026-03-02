---
template: tech
version: 0.1.0
gate: READY
---
# Tech Spec: Notifications

## Platform Scope

- Mobile (iOS + Android): KMP shared domain/data + CMP UI; `expect/actual` for push permission + token registration
- Web: web push best-effort; deferred per ADR-001
- In-app inbox works offline (local SQLDelight cache); push delivery requires connectivity

---

## Data Model

### `notifications` (in-app inbox)
| Column | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `user_id` | UUID FK → users | recipient |
| `type` | ENUM | see types below |
| `title` | TEXT | localised on server |
| `body` | TEXT | |
| `deep_link` | TEXT | e.g. `/events/{id}` |
| `reference_id` | UUID NULLABLE | event_id, user_id, etc. |
| `read` | BOOLEAN | default `false` |
| `created_at` | TIMESTAMPTZ | |

**Notification types:**
`new_event` · `event_edited` · `event_cancelled` · `event_reminder` · `attendance_response` · `attendance_summary` · `abwesenheit_change`

### `notification_settings` (1 row per user)
| Column | Type | Default |
|---|---|---|
| `user_id` | UUID PK FK → users | |
| `new_events` | BOOLEAN | `true` |
| `event_changes` | BOOLEAN | `true` |
| `event_cancellations` | BOOLEAN | `true` |
| `reminders` | BOOLEAN | `true` |
| `reminder_lead_time` | ENUM(`2h`, `1d`, `2d`) | `1d` |
| `attendance_per_response` | BOOLEAN | `true` |
| `attendance_summary` | BOOLEAN | `false` |
| `attendance_summary_lead_time` | ENUM(`2h`, `1d`, `2d`) | `2h` |
| `abwesenheit_changes` | BOOLEAN | `true` |

Row auto-created with defaults on first user login.

### `push_tokens`
| Column | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `user_id` | UUID FK → users | |
| `platform` | ENUM(`ios`, `android`, `web`) | |
| `token` | TEXT | OneSignal player/subscription ID |
| `updated_at` | TIMESTAMPTZ | upserted on each app launch |

Unique constraint: `(user_id, token)`. Multiple tokens per user supported (multi-device).

### `notification_dedup`
| Column | Type | Notes |
|---|---|---|
| `key` | TEXT PK | `sha256(user_id + type + reference_id + time_bucket)` |
| `created_at` | TIMESTAMPTZ | TTL 7 days |

Prevents duplicate delivery for rapid re-triggers.

---

## Trigger Logic

All triggers run as **background jobs** off the request path; never block API responses.

| Event | Trigger | Recipients | Setting gate |
|---|---|---|---|
| Event created | `event.created` hook | All team members | `new_events` |
| Event edited (time/location changed) | `event.updated` hook | All team members | `event_changes` |
| Event cancelled | `event.cancelled` hook | All team members; cancel pending reminders | `event_cancellations` |
| Event reminder | Scheduled job (see below) | All team members | `reminders` |
| Player attendance response | `attendance_response.upserted` hook | Event's coaches | `attendance_per_response` |
| Pre-event pending summary | Scheduled job (see below) | Event's coaches | `attendance_summary` |
| Abwesenheit added/modified | `abwesenheit_rule.upserted` hook | Team's coaches | `abwesenheit_changes` |

**Membership gate:** never send to users who are no longer active members of the event's team.

### Reminder Scheduler
- Runs every 5 minutes
- Queries: `events WHERE start_time BETWEEN now() AND now() + 5m + max_lead_time`
- For each event + member with `reminders=true`: sends if `start_time - reminder_lead_time` falls in current 5-min window
- Skips if dedup key already exists

### Pre-event Summary Scheduler
- Same cadence as reminder scheduler
- For each coach with `attendance_summary=true`: fires at `start_time - attendance_summary_lead_time`
- Payload: count of `no-response` players at time of send

### Fan-out Strategy
- Small teams (< 200 members): in-process fan-out
- Large teams: enqueue per-user send tasks to background queue

---

## Push Delivery — PushService Abstraction

**Provider: OneSignal** (current implementation). Abstracted behind `PushService` so the provider can be swapped without touching trigger logic.

### Interface (shared KMP / backend)
```kotlin
interface PushService {
    suspend fun send(
        userIds: List<UUID>,
        title: String,
        body: String,
        deepLink: String,
        data: Map<String, String> = emptyMap()
    )
}
```

### `OneSignalPushService` (active implementation)
- Uses OneSignal REST API to fan-out to registered player/subscription IDs
- Resolves `userIds` → OneSignal external user IDs (mapped 1:1 to our `user_id`)
- Web push included automatically via OneSignal (best-effort)
- Injected via DI — swap by binding a different implementation

### Token Registration
- On app launch: SDK registers device → OneSignal subscription ID stored in `push_tokens`
- On logout: `DELETE /push-tokens/{token}` + OneSignal unsubscribe
- OneSignal external user ID = our `user_id` (set on login via `OneSignal.login(userId)`)

---

## API — Ktor Backend

### Notification Inbox
| Method | Path | Description |
|---|---|---|
| GET | `/notifications` | Paginated inbox (`?page=&limit=`), newest first |
| PUT | `/notifications/{id}/read` | Mark single notification read |
| PUT | `/notifications/read-all` | Mark all read |
| DELETE | `/notifications/{id}` | Dismiss from inbox |

### Notification Settings
| Method | Path | Description |
|---|---|---|
| GET | `/notifications/settings` | Own settings |
| PUT | `/notifications/settings` | Update settings (partial update supported) |

### Push Token Registration
| Method | Path | Description |
|---|---|---|
| POST | `/push-tokens` | Register `{ platform, token }` |
| DELETE | `/push-tokens/{token}` | Deregister on logout / token rotation |

---

## KMP Architecture

- `notifications` domain module: `NotificationRepository`, `NotificationSettingsRepository`
- **Local cache**: SQLDelight mirrors `notifications` table; inbox reads from cache, synced on open
- **Unread count**: derived from local cache; no separate API call
- **`expect/actual`**:
  - `requestPushPermission(): Flow<PermissionState>` — iOS prompts system; Android 13+ prompts; older Android = auto-granted
  - `initPushSdk()` — platform-specific OneSignal SDK init; registers token, sets external user ID
  - `handleDeepLink(link: String)` — platform navigation graph resolves deep links

---

## Indexes

- `notifications(user_id, created_at DESC)` — inbox queries
- `notifications(user_id, read)` — unread count
- `push_tokens(user_id)` — fan-out lookup
- `events(start_time)` — scheduler range queries
- `notification_dedup(created_at)` — TTL cleanup
