---
template: tasks
version: 0.1.0
gate: READY GO
---
# Tasks: Compose Multiplatform Migration

## Phase 0 — Scaffold ✅ (committed c2c97ee)

| ID | Task | Deps |
|---|---|---|
| CMP-001 | Create `composeApp/` KMP module in `settings.gradle.kts`; add `build.gradle.kts` with Android + `iosSimulatorArm64` + `iosArm64` targets | — |
| CMP-002 | Add dependencies: Navigation 3 `1.0.0-alpha05`, Coil `3.4.0` + `coil-network-ktor3`, multiplatform-settings `1.3.0`, Koin `4.2.0`, `lifecycle-viewmodel`, `kotlinx-coroutines` | CMP-001 |
| CMP-003 | Create `Screen.kt` sealed class with `@Serializable` routes; add `Screen.Splash` entry point | CMP-001 |
| CMP-004 | Create empty `TeamorgApp` composable (skeleton NavHost + auth state placeholder) | CMP-003 |
| CMP-005 | Implement `AuthViewModel` reading `UserPreferences` via coroutine; emit `AuthState.Loading / Authenticated / Unauthenticated` — no `runBlocking` | CMP-002 |
| CMP-006 | expect/actual `UserPreferences` — DataStore (androidMain) / NSUserDefaults (iosMain); silent one-time migration on Android first launch | CMP-002 |
| CMP-007 | expect/actual `PushPermissionRequester` — `POST_NOTIFICATIONS` (Android) / `UNUserNotificationCenter` (iOS) | CMP-002 |
| CMP-008 | expect/actual `HapticFeedback` — `LocalHapticFeedback` (Android) / `UIImpactFeedbackGenerator` (iOS) | CMP-002 |
| CMP-009 | Wire Koin modules in `composeApp` (commonMain + androidMain + iosMain); both platforms build and launch to blank screen | CMP-004, CMP-005, CMP-006 |

> CMP-001–009 complete. Committed `c2c97ee`. See fixes below.

## Phase 0 — Fixes

| ID | Task | Deps |
|---|---|---|
| CMP-052 ✅ | Add `Screen.Register`; remove `multiplatform-settings-datastore` dep; pin OneSignal to `5.6.1` | CMP-009 |
| CMP-053 ✅ | Create `iosApp/iosApp/ContentView.swift`; XcodeGen project; `assembleXCFramework`; verify iOS sim blank screen | CMP-009 |

> **Phase 0 iOS gate PASSED.** Blank screen confirmed on iPhone 17 Pro simulator (Xcode 26.3, iOS 26.2 SDK).
> Known: `koinViewModel<AuthViewModel>()` crashes on iOS with Koin 4.0.0 — removed from `TeamorgApp` for Phase 0. Will be resolved in CMP-017 (auth gate wiring, Phase 1).

## Phase 1 — Auth + Navigation Skeleton ✅

| ID | Task | Deps |
|---|---|---|
| CMP-010 ✅ | Run pre-migration grep: `LocalContext\|LocalActivity\|rememberLauncherForActivityResult\|runBlocking` in `androidApp/`; refactor any hits | CMP-009 |
| CMP-011 ✅ | Migrate `LoginScreen` + `LoginViewModel` to `composeApp/commonMain` | CMP-010 |
| CMP-012 ✅ | `RegisterScreen` merged into `LoginScreen` (register mode toggle; YAGNI for separate screen) | CMP-010 |
| CMP-013 ✅ | Migrate `ClubSetupScreen` + `ClubSetupViewModel` (first-time onboarding; logo picker dropped — YAGNI) | CMP-010 |
| CMP-014 ✅ | Migrate `TeamSetupScreen` + `TeamSetupViewModel` (F1 flow) | CMP-010 |
| CMP-015 ✅ | Nav graph: manual `mutableStateListOf<Screen>()` backstack + `when(currentScreen)` (Navigation 3 dropped — all alphas require CMP 1.10.x / Kotlin 2.2+, incompatible with CMP 1.7.1) | CMP-011, CMP-012 |
| CMP-016 ✅ | `TeamorgBottomBar` + `Scaffold` shell; bottom bar shown on ClubDashboard + NotificationInbox | CMP-015 |
| CMP-017 ✅ | Auth gate in `TeamorgApp`: `filter { !Loading }.collect {}` pattern; `AuthViewModel.onLoginSuccess()` added | CMP-005, CMP-015 |
| CMP-018 ✅ | Deep links: `teamorg://invite?token=` in `MainActivity.onNewIntent`; `TeamorgApp` guards with auth state | CMP-017 |
| CMP-019 | Exit gate: Login → ClubDashboard flow verified on Android emulator + iOS simulator | CMP-018 |

> **Phase 1 build gate PASSED.** Android + iOS compile clean. CMP-019 (runtime verification) pending.
> Navigation 3 dropped: all alphas (01–05) transitively require CMP 1.10.x + Kotlin 2.2+. Replaced with manual backstack (`mutableStateListOf<Screen>`). Coil3 3.4.0 also dropped (compiled with Kotlin 2.3.10; no image loading needed in Phase 1).

## Stack Upgrade ✅ (committed 44bc672)

Performed between Phase 1 completion and Phase 2 start to unblock Navigation3 + Coil3.

| ID | Task | Deps |
|---|---|---|
| CMP-054 ✅ | Bump Kotlin 2.1.10→2.3.10, CMP 1.7.1→1.10.1, AGP 8.5.0→8.13.2, Gradle 8.9→8.13, kotlinx-serialization 1.7.2→1.8.0, kotlinx-coroutines 1.9.0→1.10.2, compileSdk 35→36; migrate `kotlinOptions{}` → `compilerOptions{}` across all modules; fix `compose.*` accessor deprecations in `androidApp` | CMP-018 |
| CMP-055 ✅ | Restore Navigation3 `1.0.0-alpha06` + Coil3 `3.4.0` to `composeApp/commonMain`; replace manual `when(currentScreen)` workaround with `NavDisplay`/`NavEntry` in `TeamorgApp.kt` | CMP-054 |

## Phase 2 — Team Management

| ID | Task | Deps |
|---|---|---|
| CMP-020 ✅ | Migrate `ClubDashboardScreen` + `ClubDashboardViewModel` | CMP-016 |
| CMP-021 ✅ | Migrate `TeamDetailScreen` + `TeamDetailViewModel` (roster tab) | CMP-020 |
| CMP-022 ✅ | Migrate `TeamEditSheet` (Modal bottom sheet) | CMP-021 |
| CMP-023 ✅ | Migrate `TeamInviteSheet` | CMP-021 |
| CMP-024 ✅ | Migrate `ClubEditScreen` | CMP-020 |
| CMP-025 ✅ | Migrate `PlayerProfileScreen` | CMP-021 |
| CMP-026 ✅ | Migrate `PlayerStatsScreen` | CMP-021 |
| CMP-027 ✅ | Migrate `TeamStatsScreen` | CMP-021 |
| CMP-028 ✅ | Migrate `SubgroupMgmtScreen` | CMP-021 |
| CMP-029 ✅ | Migrate `ClubCoachInviteSheet` | CMP-020 |
| CMP-030 | Exit gate: all team management screens verified on both platforms | CMP-022, CMP-023, CMP-024, CMP-025, CMP-026, CMP-027, CMP-028, CMP-029 |

## Phase 3 — Event Scheduling

| ID | Task | Deps |
|---|---|---|
| CMP-031 ✅ | Migrate `EventListScreen` + `EventListViewModel` | CMP-016 |
| CMP-032 ✅ | Migrate `EventCalendarScreen` + `EventCalendarViewModel` | CMP-031 |
| CMP-033 ✅ | Migrate `EventDetailScreen` + `EventDetailViewModel` | CMP-031 |
| CMP-034 ✅ | Migrate `EventFormScreen` + `EventFormViewModel` | CMP-031 |
| CMP-035 ✅ | Migrate `EventSubgroupMgmt` screen (N/A — no separate screen; subgroup selection handled in EventForm + existing SubgroupMgmt) | CMP-028 |
| CMP-036 ✅ | Migrate `EventTypeIndicator` shared component | CMP-031 |
| CMP-037 | Exit gate: all event scheduling screens verified on both platforms | CMP-032, CMP-033, CMP-034, CMP-035, CMP-036 |

## Phase 4 — Attendance + Stats

| ID | Task | Deps |
|---|---|---|
| CMP-038 ✅ | Migrate `AttendanceListScreen` + `AttendanceListViewModel` | CMP-016 |
| CMP-039 ✅ | Migrate `BegrundungSheet` (absence reason bottom sheet) — created in Phase 3 as prerequisite for EventDetail | CMP-038 |
| CMP-040 ✅ | Migrate `MyAbsencesScreen` + `MyAbsencesViewModel` (+ `AbsenceSheet` + `AbsenceSheetViewModel`) | CMP-038 |
| CMP-041 ✅ | Migrate `OfflineIndicator` component (`OfflineQueueBadge` + `SyncingSnackbar`); NetworkMonitor N/A — no implementation exists in shared module | CMP-016 |
| CMP-042 | Exit gate: attendance screens + offline banner verified on both platforms | CMP-039, CMP-040, CMP-041 |

## Phase 5 — Notifications

| ID | Task | Deps |
|---|---|---|
| CMP-043 ✅ | Migrate `NotificationInboxScreen` + `NotificationInboxViewModel` | CMP-016 |
| CMP-044 ✅ | Migrate `NotificationSettingsScreen` + `NotificationSettingsViewModel` (PushPermissionBanner "Open Settings" dropped — no platform way to open device settings in commonMain) | CMP-016 |
| CMP-045 ✅ | Migrate `PushPermissionScreen` (uses `PushPermissionRequester` via `koinInject`) | CMP-007 |
| CMP-046 ✅ | Wire notification badge: `NotificationRepository.getUnreadCount()` collected in `TeamorgApp`; passed to `TeamorgBottomBar` | CMP-016, CMP-043 |
| CMP-047 | Exit gate: notification screens + badge verified on both platforms | CMP-043, CMP-044, CMP-045, CMP-046 |

## Phase 6 — Cleanup

| ID | Task | Deps |
|---|---|---|
| CMP-048 ✅ | Delete all migrated UI from `androidApp/`; reduce to thin shell: `MainActivity` + `TeamorgApp` (Koin init) + `AndroidManifest.xml` only | CMP-030, CMP-037, CMP-042, CMP-047 |
| CMP-049 | Complete NT-011: OneSignal iOS SDK integration in Xcode project | CMP-047 |
| CMP-050 | Complete NT-016: Background Modes capability in Xcode + `Info.plist` | CMP-049 |
| CMP-051 | Final: `./gradlew :composeApp:assembleXCFramework` + Xcode build green; full smoke test both platforms; verify `androidApp/` is thin shell | CMP-048, CMP-050 |
