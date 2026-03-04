---
template: test-plan
version: 0.1.0
---
# Test Plan: Compose Multiplatform Migration

## Verification Strategy

Migration is complete when Android behavior is visually identical to pre-migration AND iOS runs the same Compose UI. All exit gates in tasks.md must be green before proceeding to the next phase.

---

## Phase Exit Gates

| Phase | Gate | Pass Criteria |
|-------|------|--------------|
| 0 — Scaffold | Both platforms launch | Android emulator + iOS simulator reach blank screen; no build errors |
| 1 — Auth | Login → Dashboard flow | Login with valid credentials → ClubDashboard on Android + iOS; deep link navigates correctly |
| 2 — Team Mgmt | All TM screens render | Each screen launches without crash on both platforms; data loads from shared repos |
| 3 — Events | All event screens render | EventList, Calendar, Detail, Form, SubgroupMgmt functional on both platforms |
| 4 — Attendance | Attendance screens render | AttendanceList, BegrundungSheet, MyAbsences, OfflineIndicator functional on both platforms |
| 5 — Notifications | Notification screens render | InboxScreen, SettingsScreen, PushPermissionScreen functional; badge shows unread count |
| 6 — Cleanup | Thin shell + clean build | `androidApp/` contains only `MainActivity` + manifest; `./gradlew :composeApp:assembleXCFramework` + Xcode build green |

---

## Regression Tests (Android)

Run after each phase on Android emulator before marking exit gate done.

| # | Flow | Expected |
|---|------|----------|
| R-01 | Login with valid credentials | Navigates to ClubDashboard |
| R-02 | Login with invalid credentials | Inline error shown |
| R-03 | Tap team in ClubDashboard | TeamDetail screen opens |
| R-04 | Open TeamInviteSheet, submit | Invite created, sheet closes |
| R-05 | Navigate to EventList tab | Events render; calendar tab reachable |
| R-06 | Confirm attendance on event | Badge updates to CONFIRMED |
| R-07 | Open BegrundungSheet | Submits absence reason, sheet closes |
| R-08 | Notification badge | Shows unread count; tap → NotificationInbox |
| R-09 | Force offline | OfflineIndicator banner visible |
| R-10 | Accept invite deep link | Navigates to InviteAccept → team roster updated |

---

## Smoke Tests (iOS)

Verified manually on iOS 16+ simulator after Phase 1 exit gate.

| # | Flow | Expected |
|---|------|----------|
| S-01 | App launch | Splash → Login (no stored token) |
| S-02 | Login with valid credentials | Navigates to ClubDashboard; "Meine Teams" visible |
| S-03 | Bottom navigation | All tabs reachable via bottom bar |
| S-04 | Team list → Team detail | Tap team row → TeamDetail with roster |
| S-05 | Event list | Events render; tap → EventDetail |
| S-06 | Confirm attendance | Attendance status badge updates |
| S-07 | Notification inbox | Inbox opens; entries visible |
| S-08 | System back / swipe-from-left | Navigates back correctly |
| S-09 | Push permission prompt | Platform permission dialog appears (PushPermissionScreen) |
| S-10 | Haptic on attendance confirm | Tactile feedback on physical device |

---

## Edge Cases

| # | Scenario | Expected |
|---|----------|----------|
| E-01 | Token expiry during session (401) | Auto-logout → Login screen |
| E-02 | No internet at app start | OfflineIndicator shown; cached data displayed |
| E-03 | Deep link while unauthenticated | Login first → then navigate to invite destination |
| E-04 | XCFramework stale after Kotlin change | Re-run `assembleXCFramework`; Xcode resolves updated framework |
| E-05 | DataStore → multiplatform-settings migration | Existing Android users not prompted to log in again after upgrade |
| E-06 | `runBlocking` in any migrated code | Must not exist in commonMain — causes iOS deadlock |

---

## Tools

| Tool | Purpose |
|------|---------|
| `./gradlew :composeApp:assembleDebug` | Android smoke build |
| `./gradlew :composeApp:assembleXCFramework` | iOS framework build |
| Android emulator (API 34) | Android smoke tests |
| iOS 16 simulator | iOS smoke tests |
| `grep -r "runBlocking\|LocalContext\|LocalActivity" composeApp/src/commonMain/` | Safety check before Phase 6 |
