---
template: test-plan
version: 0.1.0
---
# Test Plan: Notifications

## Unit Tests

### Backend

| # | Subject | Cases |
|---|---|---|
| U-01 | `dedup helper` | same key returns true on second call; different `timeBucket` returns false; TTL expiry allows re-send |
| U-02 | `resolveRecipients()` | returns only active members; excludes users with setting disabled; excludes removed members |
| U-03 | `OneSignalPushService` chunking | 2,500 user IDs → two API calls (2,000 + 500); each call contains correct slice |
| U-04 | `event.created` hook setting gate | user with `new_events = false` not in recipients; user with `new_events = true` is |
| U-05 | `reminder_lead_time` matching | user with `2h` lead time only gets notification when `start_at - 2h` falls in current 5-min window |
| U-06 | Reminder dedup | second scheduler pass for same event+user returns dedup hit; no second send |
| U-07 | Auto-create settings | first login inserts row with all defaults; second login does not duplicate |
| U-08 | Membership gate | user removed from team after event created → not in recipients for reminder |

### KMP Domain

| # | Subject | Cases |
|---|---|---|
| U-09 | Unread count | `SELECT COUNT` returns correct count; decrements on `markRead` |
| U-10 | Local cache sync | upsert merges server response; does not duplicate on second sync |
| U-11 | Deep link routing | `"/events/abc"` → `EventDetailDestination("abc")`; unknown path → no-op / fallback |

---

## Integration Tests

| # | Flow | Assertions |
|---|---|---|
| I-01 | Create event → trigger notification | `notifications` row created for each targeted player; `push_tokens` queried; OneSignal API called |
| I-02 | Edit event (location changed) → notification sent | `event_changes` setting respected; dedup key written |
| I-03 | Cancel event → notifications sent + reminders cancelled | `event_cancellations` gate; dedup keys for pending reminders marked stale |
| I-04 | Player responds to attendance → coaches notified | `attendance_per_response` gate; only coaches of that event receive it |
| I-05 | `PUT /notifications/settings` disables `new_events` → user excluded from next event notification | Settings updated; `resolveRecipients` excludes user |
| I-06 | `PUT /notifications/read-all` → `GET /notifications` returns all `read: true` | All rows updated |
| I-07 | Register push token → deregister on logout | `push_tokens` row inserted; deleted after `DELETE /push-tokens/{token}` |
| I-08 | Reminder scheduler — correct lead time window | Event at T+2h; user has `reminder_lead_time = 2h`; scheduler at T fires notification |
| I-09 | Pre-event summary to coach | At `start_at - summary_lead_time`: coach receives summary with `no-response` count |
| I-10 | Fan-out > 2,000 users | 2,500 targeted users → OneSignal called twice; all users receive notification |

---

## Edge Case Tests

| # | Scenario | Expected |
|---|---|---|
| E-01 | Event cancelled before reminder fires | Reminder skipped (dedup or event status check) |
| E-02 | Push token rotation (OS reissues token) | New `POST /push-tokens` upserts; old token eventually stops receiving |
| E-03 | User has 3 devices | All 3 `push_tokens` rows included in OneSignal payload |
| E-04 | Scheduler fires twice in same 5-min window (crash/restart) | Dedup key prevents double-send |
| E-05 | No push tokens for user | Notification row still written to inbox; no OneSignal call for that user |
| E-06 | All users have `new_events = false` | `resolveRecipients` returns empty; no push sent; no notifications written |

---

## Manual / E2E Tests

| # | Journey | Steps |
|---|---|---|
| M-01 | Push notification received on Android | Create event → physical Android device receives push; tap → opens event detail |
| M-02 | Push notification received on iOS | Same flow on iOS simulator or device |
| M-03 | Permission denied flow | Deny permission on iOS → warning banner shown in app; inbox still works |
| M-04 | Deep link cold start | Kill app; tap push notification → app opens directly on correct screen |
| M-05 | Unread badge lifecycle | Receive 3 notifications → badge shows 3; open inbox → badge clears |
| M-06 | Settings toggle respected | Disable `new_events` → create event → no push received; re-enable → next event sends push |
