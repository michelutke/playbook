---
template: tasks
version: 0.1.0
gate: READY GO
---
# Tasks: Notifications

> **Depends on:** team-management, event-scheduling, attendance-tracking complete first.

## Phase 1 — Database Schema

| ID | Task | Deps |
|---|---|---|
| NT-001 | Create `notifications` table (id, user_id, type, title, body, deep_link, reference_id, read, created_at) | — |
| NT-002 | Create `notification_settings` table with all columns + defaults | — |
| NT-003 | Create `push_tokens` table — unique `(user_id, token)` | — |
| NT-004 | Create `notification_dedup` table (key TEXT PK, created_at) | — |
| NT-005 | Add indexes: `notifications(user_id, created_at DESC)`, `notifications(user_id, read)`, `push_tokens(user_id)` | NT-001, NT-003 |

## Phase 2 — OneSignal SDK Integration

### Android (androidMain)
| ID | Task | Deps |
|---|---|---|
| NT-006 | Add OneSignal Android SDK 5.6.1+ to Android Gradle | — |
| NT-007 | `initPushSdk()` actual — `OneSignal.initWithContext(context, appId)` in `Application.onCreate` | NT-006 |
| NT-008 | `requestPushPermission()` actual — `OneSignal.Notifications.requestPermission(fallbackToSettings)` | NT-006 |
| NT-009 | Store `OneSignal.User.pushSubscription.id` → `POST /push-tokens` on app launch | NT-007 |
| NT-010 | `OneSignal.login(userId)` on auth; `OneSignal.logout()` on sign-out | NT-007 |

### iOS (iosMain)
| ID | Task | Deps |
|---|---|---|
| NT-011 | Add OneSignal XCFramework 5.x via Swift Package Manager | — |
| NT-012 | Create `OneSignalWrapper.swift` ObjC-bridged shim for KMP interop | NT-011 |
| NT-013 | `initPushSdk()` actual — calls shim → `OneSignal.initialize(appId, launchOptions)` | NT-012 |
| NT-014 | `requestPushPermission()` actual — `OneSignal.Notifications.requestPermission(callback)` | NT-012 |
| NT-015 | `OneSignal.login(userId)` / `logout()` via shim | NT-012 |
| NT-016 | Enable "Background Modes → Remote notifications" capability in Xcode | — |

### KMP expect/actual
| ID | Task | Deps |
|---|---|---|
| NT-017 | Define `expect fun initPushSdk()` in commonMain | — |
| NT-018 | Define `expect fun requestPushPermission(): Flow<PermissionState>` | — |
| NT-019 | Define `expect fun handleDeepLink(link: String)` | — |
| NT-020 | Wire Android `actual` implementations (NT-007, NT-008) | NT-017, NT-018, NT-007, NT-008 |
| NT-021 | Wire iOS `actual` implementations (NT-013, NT-014) | NT-017, NT-018, NT-013, NT-014 |

## Phase 3 — Ktor Backend

### Infrastructure
| ID | Task | Deps |
|---|---|---|
| NT-022 | Define `PushService` interface: `suspend fun send(userIds, title, body, deepLink, data)` | — |
| NT-023 | Implement `OneSignalPushService` — Ktor HTTP client → `POST https://api.onesignal.com/notifications`; chunk at 2,000 external IDs | NT-022 |
| NT-024 | Register `PushService` as Koin singleton | NT-023 |
| NT-025 | Implement `resolveRecipients(eventId, type)` — targeted users × membership × notification setting | NT-002 |
| NT-026 | Implement dedup helper: write/check `notification_dedup.key` = `sha256(userId+type+refId+timeBucket)` | NT-004 |
| NT-027 | Auto-create `notification_settings` row with defaults on first user login | NT-002 |

### Inbox endpoints
| ID | Task | Deps |
|---|---|---|
| NT-028 | `GET /notifications` — paginated, newest first | NT-001 |
| NT-029 | `PUT /notifications/{id}/read` — mark single read | NT-001 |
| NT-030 | `PUT /notifications/read-all` — mark all read | NT-001 |
| NT-031 | `DELETE /notifications/{id}` — dismiss | NT-001 |

### Settings endpoints
| ID | Task | Deps |
|---|---|---|
| NT-032 | `GET /notifications/settings` — own settings | NT-002 |
| NT-033 | `PUT /notifications/settings` — partial update (upsert) | NT-002 |

### Push token endpoints
| ID | Task | Deps |
|---|---|---|
| NT-034 | `POST /push-tokens` — register `{ platform, token }`; upsert `(user_id, token)` | NT-003 |
| NT-035 | `DELETE /push-tokens/{token}` — deregister on logout | NT-003 |

## Phase 4 — Trigger Hooks & Schedulers

| ID | Task | Deps |
|---|---|---|
| NT-036 | `event.created` hook → background coroutine → notify targeted players (gate: `new_events`) | NT-024, NT-025, NT-026 |
| NT-037 | `event.updated` hook → notify if time/location changed (gate: `event_changes`) | NT-036 |
| NT-038 | `event.cancelled` hook → notify players + cancel pending reminder dedup entries (gate: `event_cancellations`) | NT-036 |
| NT-039 | `attendance_response.upserted` hook → notify event's coaches (gate: `attendance_per_response`) | NT-025, NT-026 |
| NT-040 | `abwesenheit_rule.upserted` hook → notify team's coaches (gate: `abwesenheit_changes`) | NT-025, NT-026 |
| NT-041 | Reminder scheduler: coroutine `while(isActive) { checkReminders(); delay(5.minutes) }` | NT-024, NT-026 |
| NT-042 | Reminder query: events with `start_at` matching `now + lead_time` in 5-min window; dedup + send | NT-041 |
| NT-043 | Pre-event summary scheduler: same 5-min loop; fires at `start_at - summary_lead_time` for coaches | NT-041, NT-039 |
| NT-044 | Membership gate: filter out users no longer active team members before sending | NT-025 |

## Phase 5 — KMP Domain Layer

| ID | Task | Deps |
|---|---|---|
| NT-045 | Define `NotificationRepository` interface + impl (inbox CRUD) | NT-028 |
| NT-046 | Define `NotificationSettingsRepository` interface + impl | NT-032 |
| NT-047 | SQLDelight schema: `notifications_cache` table mirroring server `notifications` | NT-001 |
| NT-048 | Local cache sync: fetch from `GET /notifications` on inbox open; upsert to SQLDelight | NT-045, NT-047 |
| NT-049 | Unread count: `SELECT COUNT(*) WHERE read = 0` from local cache; no extra API call | NT-047 |
| NT-050 | Deep link routing map: `"/events/{id}"` → Event Detail, `"/teams/{id}"` → Team Detail, etc. | NT-019 |
| NT-051 | Call `initPushSdk()` in app startup before user session established | NT-017 |

## Phase 6 — Mobile UI (CMP)

| ID | Task | Deps |
|---|---|---|
| NT-052 | Notification inbox screen — paginated list; unread badge; mark-read on tap; swipe-dismiss | NT-045 |
| NT-053 | Notification settings screen — toggle per type; lead time picker for reminders + summaries | NT-046 |
| NT-054 | Push permission prompt screen — shown post-onboarding; denied → persistent banner | NT-018 |
| NT-055 | Unread badge on inbox nav icon (derived from NT-049 count) | NT-049 |
| NT-056 | In-app fallback snackbar: "Notifications disabled — enable in settings" (shown once on denial) | NT-054 |
| NT-057 | Deep link cold-start handling: app navigates to correct screen from push tap | NT-050 |
| NT-058 | Deep link warm-start handling: app already open; navigate without restart | NT-050 |
