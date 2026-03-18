---
template: plan
version: 0.1.0
gate: READY GO
---
# Plan: Compose Multiplatform Migration

## Overview

Move all Compose UI from `androidApp/` to a new `composeApp/` KMP module targeting Android + iOS. 7 phases, each producing a working build before proceeding. ~22 ViewModels, ~80 composable files total.

---

## Phase 0 — Scaffold
**Gate:** Android + iOS simulator both launch to blank screen (no crash)

### Tasks
1. **Create `composeApp/` KMP module**
   - `composeApp/build.gradle.kts` with `kotlin { androidTarget(); iosSimulatorArm64(); iosArm64() }`
   - Add to `settings.gradle.kts`
   - Dependency graph: `androidApp → composeApp → shared`

2. **Add all libs to `composeApp`**
   - `commonMain`: Navigation 3 `1.0.0-alpha05`, Coil 3.4.0 + `coil-network-ktor3`, `multiplatform-settings:1.3.0` + `datastore` variant, `koin-compose:4.2.0`, `koin-compose-viewmodel:4.2.0`, `lifecycle-viewmodel:2.8.7`, Material Icons Extended, `kotlinx-coroutines`
   - `androidMain`: `activity-compose`, `koin-android`, `lifecycle-runtime-compose`
   - Update `libs.versions.toml` with all new versions

3. **Empty entry points**
   - `composeApp/src/commonMain/.../TeamorgApp.kt` — `@Composable fun TeamorgApp()` (empty box)
   - `composeApp/src/iosMain/.../MainViewController.kt` — `fun MainViewController(): UIViewController = ComposeUIViewController { TeamorgApp() }`

4. **`AuthViewModel` + `AuthState` (commonMain)**
   - `sealed interface AuthState { Loading; Unauthenticated; data class Authenticated(val clubId: String) }`
   - `AuthViewModel` reads `UserPreferences` asynchronously in `init { viewModelScope.launch {} }`
   - Exposes `StateFlow<AuthState>`

5. **`Screen.Splash` as initial nav entry** (empty composable, no visible UI)

6. **`UserPreferences` expect/actual**
   - Interface in `composeApp/commonMain`
   - `androidMain`: `DataStoreSettings` wrapper + one-time migration from old DataStore
   - `iosMain`: `NSUserDefaultsSettings` wrapper

7. **`PushPermissionRequester` expect/actual** (stubs only at this phase)

8. **`HapticFeedback` expect/actual** (stubs only at this phase)

9. **`UiModule` Koin module** (empty, just `AuthViewModel` for now)

10. **`androidApp/` thin-shell wiring**
    - `MainActivity` calls `setContent { TeamorgApp() }`
    - `TeamorgApp.kt` `startKoin {}` includes `SharedModule + UiModule + AndroidPlatformModule`
    - Verify Android still builds and launches

11. **Xcode project setup (iosApp/)**
    - Delete placeholder skeleton; run KMP wizard or manually configure Xcode project
    - Set minimum iOS target: 16.0
    - Set `ARCHS = arm64` for simulator build setting
    - Run `./gradlew :composeApp:assembleXCFramework`
    - Embed `composeApp.xcframework` in Xcode → Frameworks, Libraries, and Embedded Content
    - `ContentView.swift`: `MainViewControllerKt.MainViewController()`
    - Add `teamorg://` URL scheme to `Info.plist`
    - Verify iOS simulator launches to blank screen

**Exit gate:** `./gradlew :androidApp:assembleDebug` succeeds; iOS simulator shows blank screen

---

## Phase 1 — Auth + Navigation Skeleton
**Gate:** Login → ClubDashboard flow works on both platforms; deep link wiring functional

### Pre-flight (run before moving anything)
```bash
grep -r "LocalContext" androidApp/src/
grep -r "LocalActivity" androidApp/src/
grep -r "rememberLauncherForActivityResult" androidApp/src/
grep -r "runBlocking" androidApp/src/
```
Each hit in a screen that will move to `commonMain` must be resolved first.

### Tasks
1. **Migrate `Screens.kt`** → `composeApp/commonMain` (serializable route objects, unchanged)
2. **Migrate `TeamorgNavGraph.kt`** → rewrite for Navigation 3 `NavDisplay` / `BackStack`
   - Replace `rememberNavController` → `rememberNavBackStack(Screen.Splash)`
   - Remove `runBlocking` → `AuthViewModel` drives navigation via `LaunchedEffect(authState)`
3. **Migrate `TeamorgBottomBar`** → `composeApp/commonMain`
4. **Migrate auth screens** (`LoginScreen`, `ClubSetupScreen`, `CoachFirstTeamSetupScreen`, `InviteAcceptScreen`) + their ViewModels
5. **Coil 3 init** — `SingletonImageLoader.setSafe(context)` or `setSingletonImageLoaderFactory` in `androidMain`
6. **Deep link handling**
   - Android: `MainActivity.onNewIntent` → extract `teamorg://invite?token=` → pass to `TeamorgApp`
   - iOS: URL scheme handler in `SceneDelegate` or `ContentView` → pass `deepLinkToken` to `MainViewController`
   - `TeamorgApp` `LaunchedEffect(deepLinkToken, authState)` handles auth-guard flow

**Exit gate:** Login/register/invite-accept flow works on Android + iOS simulator

---

## Phase 2 — Team Management
**Gate:** All team features work on iOS simulator

### Screens + ViewModels to migrate
- `ClubDashboard` + VM
- `TeamDetail` + VM
- `TeamEditSheet` + VM
- `TeamInviteSheet` + VM
- `ClubEditScreen` + VM
- `PlayerProfile` + VM
- `PlayerStats` + VM
- `TeamStats` + VM
- `SubgroupMgmt` + VM
- `ClubCoachInviteSheet` + VM
- Shared components: `StatusBadge`, `InviteStatusBadge`, `RoleChip`

### Tasks
1. For each screen: copy to `composeApp/commonMain`, fix any `LocalContext` hits, update imports to coil3
2. Register all new ViewModels in `UiModule`
3. Add routes to `TeamorgNavGraph`

**Exit gate:** Team features functional on iOS sim; Android regression clean

---

## Phase 3 — Event Scheduling
**Gate:** All event features work on iOS simulator

### Screens + ViewModels to migrate
- `EventListScreen` + VM
- `EventCalendarScreen` + VM
- `EventDetailScreen` + VM
- `EventFormScreen` + VM
- `EventSubgroupMgmt` + VM
- `EventTypeIndicator` component

**Exit gate:** Event create/edit/view works on iOS sim

---

## Phase 4 — Attendance + Stats
**Gate:** All attendance features work on iOS simulator

### Screens + ViewModels to migrate
- `AttendanceListScreen` + VM
- `BegrundungSheet` + VM
- `MyAbsencesScreen` + VM
- `PlayerStatsScreen` (if not already moved in Phase 2)
- `TeamStatsScreen` (if not already moved in Phase 2)
- `OfflineIndicator` component
- `NetworkMonitor` expect/actual (if not already in `shared/`)

**Exit gate:** Confirm/decline attendance + absences work on iOS sim

---

## Phase 5 — Notifications
**Gate:** Notification inbox + push permission flow works on iOS simulator

### Screens + ViewModels to migrate
- `NotificationInboxScreen` + VM
- `NotificationSettingsScreen` + VM
- `PushPermissionScreen` + VM

### Tasks
1. Migrate screens to `commonMain`
2. **`PushPermissionRequester` actual implementations** (replace Phase 0 stubs)
   - `androidMain`: `POST_NOTIFICATIONS` permission request
   - `iosMain`: `UNUserNotificationCenter.requestAuthorization`
3. **`HapticFeedback` actual implementations**
   - `androidMain`: `LocalHapticFeedback`
   - `iosMain`: `UIImpactFeedbackGenerator`
4. Bottom nav notification badge wiring

**Exit gate:** Push permission flow works on iOS sim; badge shows unread count

---

## Phase 6 — Cleanup + iOS Native Wiring
**Gate:** `androidApp/` contains no screen/component Compose code; Android regression clean; NT-011 + NT-016 complete

### Tasks
1. **Delete all migrated screen/VM code from `androidApp/src/`**
   - Keep: `MainActivity.kt`, `TeamorgApp.kt`, `di/AndroidPlatformModule.kt`, `push/` OneSignal wiring
2. **Verify `androidApp/` is thin shell** — no `@Composable` fun except entry point
3. **NT-011**: Add OneSignal iOS SDK via SPM in Xcode
4. **NT-016**: Enable Background Modes → Remote notifications in Xcode
5. **Full Android regression pass** — all 5 feature areas, deep links, offline mode
6. **iOS smoke pass** — all 5 feature areas on simulator

**Exit gate:** All acceptance criteria from req.md met

---

## Key Risks (watch during implementation)

| Risk | Watch for |
|------|-----------|
| Navigation 3 alpha API shifts | Don't upgrade `navigation3-ui` mid-sprint; pin `1.0.0-alpha05` |
| `runBlocking` in NavGraph | Eliminated in Phase 1 — verify no deadlock on iOS after Phase 1 |
| `LocalContext` in composables | Pre-flight grep in each phase; each hit needs expect/actual or `androidMain` move |
| XCFramework stale after Kotlin changes | Run `./gradlew :composeApp:assembleXCFramework` before every Xcode build |
| DataStore → multiplatform-settings migration | Silent one-time migration in `DataStoreSettings.init` — verify no re-login required |
| `composeApp/` Robolectric config | Add `isIncludeAndroidResources = true` in Phase 0 scaffold (needed for test-suite) |
