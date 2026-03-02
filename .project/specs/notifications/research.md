---
template: research
version: 0.1.0
---
# Research: Notifications

## OneSignal Android SDK
- **Version**: 5.6.1+
- **Docs**: https://documentation.onesignal.com/docs/en/android-sdk-setup
- **KMP support**: partial — native Android SDK; called via `expect/actual` from KMP
- **Key findings**:
  - SDK 5.x is the current major version (major breaking release from 4.x)
  - `OneSignal.initWithContext(context, appId)` replaces old init pattern
  - `OneSignal.login(externalId)` maps device to our `user_id` — exact pattern we need
  - `OneSignal.logout()` on user sign-out; removes external ID association
  - Notification permission: `OneSignal.Notifications.requestPermission(fallbackToSettings, callback)` — Android 13+ required
  - Push token (subscription ID) retrieved via `OneSignal.User.pushSubscription.id`
  - Token must be stored in our `push_tokens` table on login/launch
  - No direct KMP artifact; integrate via Android `actual` implementation
- **Decision**: **use** — specified provider; abstracted behind `PushService` interface

## OneSignal iOS SDK
- **Version**: 5.x (Swift; available via Swift Package Manager / CocoaPods)
- **Docs**: https://documentation.onesignal.com/docs/en/ios-sdk-setup
- **KMP support**: partial — native Swift/ObjC SDK; called via iOS `actual` implementation
- **Key findings**:
  - SPM: add `https://github.com/OneSignal/OneSignal-XCFramework` to Xcode project
  - `OneSignal.initialize(appId, launchOptions: launchOptions)` in `AppDelegate`
  - `OneSignal.login(externalId)` — same API shape as Android SDK 5.x (symmetrical)
  - Permission request: `OneSignal.Notifications.requestPermission(callback)` — triggers iOS system prompt
  - Push token: `OneSignal.User.pushSubscription.id` — same as Android
  - KMP integration: iOS `actual` class calls Swift OneSignal APIs via `@ObjCName` / interop layer
  - Gotcha: must be initialised in `AppDelegate`/`@main` before KMP code runs
- **Decision**: **use** — platform `actual` implementation alongside Android

## OneSignal REST API (server-side)
- **Docs**: https://documentation.onesignal.com/reference/create-notification
- **KMP support**: N/A (Ktor backend, JVM)
- **Key findings**:
  - Endpoint: `POST https://api.onesignal.com/notifications` with `Authorization: Key <REST_API_KEY>`
  - Target by external user ID: `"include_aliases": { "external_id": ["user_id_1", "user_id_2"] }`
  - Batch limit: up to 2,000 external IDs per request — fan-out logic must chunk large teams
  - Rate limits: generous for typical usage; check dashboard for plan limits
  - Deep link: `"url"` field carries the in-app deep link path
  - Data payload: `"data": { "type": "event_created", "ref_id": "..." }` for client-side routing
  - Web push: included automatically for registered web subscribers — no extra integration needed
  - Server sends via Ktor's HTTP client (`io.ktor:ktor-client-cio`) against OneSignal REST
- **Decision**: **use** — straightforward REST; abstract behind `PushService` to allow future provider swap

## Ktor Periodic Scheduler (5-minute reminder/summary jobs)
- **Version**: Ktor 3.4.0
- **Docs**: https://flaxoos.github.io/extra-ktor-plugins/ktor-server-task-scheduling/
- **KMP support**: N/A (server JVM)
- **Key findings**:
  - Reminder + pre-event summary schedulers run every 5 minutes
  - **Raw coroutine approach**: `launch { while(true) { checkReminders(); delay(5.minutes) } }` — simplest, zero deps
  - **extra-ktor-plugins task scheduling**: Ktor plugin with JDBC lock; safer for multi-instance (prevents duplicate sends)
  - Dedup table (`notification_dedup`) already designed to prevent duplicates — raw coroutines + dedup key is safe for single-instance
  - For multi-instance: extra-ktor-plugins JDBC lock prevents double-send across instances
  - Job is read-heavy (queries events + settings); no write contention concerns for single-instance
- **Decision**: **raw coroutines + dedup table for MVP**; **extra-ktor-plugins** if multi-instance
