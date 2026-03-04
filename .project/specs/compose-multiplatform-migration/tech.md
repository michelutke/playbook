---
template: tech
version: 0.1.0
gate: READY GO
---
# Technical Design: Compose Multiplatform Migration

## Architecture

### Module Structure (after migration)

```
/
├── shared/        — unchanged: KMP domain + data layer, SQLDelight, Ktor client
├── composeApp/    — NEW: KMP UI module (Android + iOS targets)
│   └── src/
│       ├── commonMain/  — all screens, ViewModels, navigation, shared components
│       ├── androidMain/ — expect/actual: DataStore settings, push permission, haptic
│       └── iosMain/     — expect/actual: NSUserDefaults settings, push permission, haptic
├── androidApp/    — THIN SHELL: MainActivity, PlaybookApp (Koin init), Android manifest
├── iosApp/        — PROPER XCODE PROJECT: SwiftUI ContentView wrapping CMP root composable
├── backend/       — unchanged
└── admin/         — unchanged
```

**Dependency graph:**
```
androidApp → composeApp → shared
iosApp (Xcode) → composeApp.xcframework (local SPM / embedded) → shared.xcframework
```

### iOS Framework Integration

**Approach: XCFramework embedded via XcodeGen**
- `composeApp` Gradle config declares `framework {}` block (no `cocoapods {}`)
- Build XCFramework via `./gradlew :composeApp:assembleXCFramework`
- Output: `composeApp/build/XCFrameworks/debug/composeApp.xcframework` (debug) / `release/`
- **Xcode project is generated from `iosApp/project.yml`** via `xcodegen generate` — never edit `.xcodeproj` directly
- `project.yml` embeds `debug/composeApp.xcframework` + links `libsqlite3.tbd` (required by SQLDelight's `NativeSqliteDriver`)
- `iosApp/iosApp/ContentView.swift` wraps `MainViewControllerKt.MainViewController()`
- `iosApp/iosApp/iOSApp.swift` — SwiftUI `@main` struct
- No `Podfile`, no `pod install` — zero CocoaPods involvement

**Workflow after any Kotlin change:**
```bash
./gradlew :composeApp:assembleXCFramework   # rebuild framework
xcodegen generate                           # regenerate .xcodeproj (if project.yml changed)
# then build in Xcode
```

**Minimum iOS target:** 16.0 (CMP minimum; aligns with NT-011/NT-016 requirements)

### DI Strategy

```
shared/         → SharedModule (repositories, use-cases, Ktor, SQLDelight)
composeApp/     → UiModule (all ViewModels)
androidApp/     → AndroidPlatformModule (DataStore, OneSignal, push token manager)
iosApp/ (Swift) → iOSPlatformModule (NSUserDefaults, OneSignal iOS SDK)
```

`startKoin {}` called at each platform entry point with the full module list.

**iOS startup sequence** (actual implementation): `startKoin` is called inside `MainViewController()` (Kotlin, `iosMain`) before returning the `ComposeUIViewController`. Guarded by a `private var koinInitialized` flag to prevent double-init. Modules: `iosPlatformModule()` + `sharedModule(ApiConfig(...))` + `uiModule` + `iosComposeModule`. The `authTokenProvider` lambda reads `NSUserDefaults` synchronously (no suspend needed).

### `androidApp/` Thin Shell (post-migration contents)
- `MainActivity.kt` — `setContent { /* composeApp root composable */ }`
- `PlaybookApp.kt` — Application class, `startKoin {}`, OneSignal Android init
- `di/AndroidPlatformModule.kt` — DataStore, push token manager bindings
- `push/` — OneSignal Android SDK wiring (unchanged)
- `preferences/UserPreferencesImpl.android.kt` — DataStore implementation

---

## Library Compatibility Matrix

| Library | Current | CMP iOS | Action |
|---------|---------|---------|--------|
| `androidx.navigation:navigation-compose` | 2.8.4 | ✗ Android-only | **Migrate to Navigation 3** (see Decisions) |
| `io.coil-kt:coil-compose` | 2.7.0 | ✗ | **Upgrade to Coil 3.4.0** (`io.coil-kt.coil3`) |
| `androidx.datastore:datastore-preferences` | 1.1.1 | ✗ | **Stays in `androidMain` actual of `UserPreferences`** (see D3) |
| `com.onesignal:OneSignal` (Android SDK) | 5.6.1 | ✗ | Stays in `androidApp/`; iOS SDK via SPM (NT-011) |
| `io.insert-koin:koin-compose` | 4.0.0 → **4.2.0** | ✓ | Move to `commonMain` via Koin BOM 4.2.0 |
| `io.insert-koin:koin-compose-viewmodel` | 4.0.0 → **4.2.0** | ✓ | Move to `commonMain` |
| `io.insert-koin:koin-androidx-compose` | 4.0.0 | ✗ (Android-only name) | Drop; `koin-compose` covers it |
| `io.insert-koin:koin-android` | 4.0.0 → **4.2.0** | n/a | Stays in `androidApp/` |
| `androidx.lifecycle:lifecycle-viewmodel-ktx` | 2.8.7 | ✓ (core) | Move `lifecycle-viewmodel` to `commonMain` |
| `androidx.lifecycle:lifecycle-runtime-compose` | 2.8.7 | ✗ | Replace with `collectAsStateWithLifecycle` from `lifecycle-runtime-compose` for Android; plain `collectAsState` in `commonMain` |
| `androidx.activity:activity-compose` | 1.9.3 | ✗ | Stays in `androidApp/` thin shell |
| Material Icons Extended | 1.7.6 | ✓ | Move to `commonMain` |
| `kotlinx-coroutines` | 1.9.0 | ✓ | Already in `shared/`; add to `composeApp/commonMain` |
| `kotlinx-datetime` | 0.6.0 | ✓ | Already in `shared/`; expose via shared module |

### expect/actual Surface

| Interface | androidMain | iosMain |
|-----------|-------------|---------|
| `UserPreferences` (token, user ID, clubId) | DataStore (`DataStoreSettings`) | NSUserDefaults (`NSUserDefaultsSettings`) |
| `PushPermissionRequester` | POST_NOTIFICATIONS permission request | `UNUserNotificationCenter.requestAuthorization` |
| `HapticFeedback` | `LocalHapticFeedback` (Compose) | `UIImpactFeedbackGenerator` via `UIKitInteropKit` |

---

## Data Model

No schema changes. No new backend API. No new SQLDelight tables.

`UserPreferences` key names are preserved; underlying storage changes per platform (DataStore on Android → NSUserDefaults on iOS). Migration of existing Android DataStore values to the new `multiplatform-settings` wrapper: implement a one-time migration in `DataStoreSettings` init on first launch.

---

## API / Interfaces

No new backend endpoints. The migration is purely client-side.

### New `composeApp/commonMain` public surface

```kotlin
// composeApp entry point composable (called from both platforms)
// Internally starts at Screen.Splash and resolves auth state asynchronously
@Composable
fun PlaybookApp(windowSizeClass: WindowSizeClass? = null)

// iOS entry point (called from Swift)
fun MainViewController(): UIViewController =
    ComposeUIViewController { PlaybookApp() }
```

### Initial Auth State — Async Pattern (replaces `runBlocking`)

The current `PlaybookNavGraph.kt` uses `runBlocking` to read the auth token synchronously and pick the start destination. **This will deadlock on iOS** (iOS main thread cannot block). Replaced with:

```kotlin
// Screen.Splash is the initial back stack entry — renders nothing / app icon
sealed interface AuthState { object Loading; object Unauthenticated; data class Authenticated(val clubId: String) : AuthState() }

@Composable
fun PlaybookApp(...) {
    val authState by authViewModel.authState.collectAsState()
    val backStack = rememberNavBackStack(Screen.Splash)

    LaunchedEffect(authState) {
        when (val s = authState) {
            AuthState.Loading -> Unit  // stay on Splash
            AuthState.Unauthenticated -> backStack.replaceAll(Screen.Login)
            is AuthState.Authenticated -> backStack.replaceAll(Screen.ClubDashboard(s.clubId))
        }
    }
    NavDisplay(backStack = backStack, ...) { ... }
}
```

`AuthViewModel` (commonMain) reads `UserPreferences` via coroutine on init. No `runBlocking` anywhere in shared code.

### Deep Link Handling

- `playbook://invite?token={token}` — registered in Android `AndroidManifest.xml` (unchanged)
- iOS: URL scheme `playbook://` registered in `Info.plist`

**Navigation 3 deep link pattern** (differs from AndroidX Navigation — no automatic route resolution):

*Android (`MainActivity.kt` in thin shell):*
```kotlin
// In MainActivity.onCreate / onNewIntent
intent?.data?.let { uri ->
    if (uri.scheme == "playbook" && uri.host == "invite") {
        val token = uri.getQueryParameter("token")
        deepLinkToken = token  // passed into PlaybookApp as parameter or via ViewModel
    }
}
```

*In `PlaybookApp` commonMain:*
```kotlin
LaunchedEffect(deepLinkToken, authState) {
    if (deepLinkToken != null) {
        if (authState is AuthState.Unauthenticated) {
            backStack.replaceAll(Screen.Login)
            // LoginViewModel stores pending token; on success navigates to InviteAccept
        } else {
            backStack.add(Screen.InviteAccept(deepLinkToken))
        }
    }
}
```

*iOS (`ContentView.swift`):*
```swift
MainViewControllerKt.MainViewController(deepLinkToken: extractedToken)
// or via URL scheme handler in SceneDelegate
```

- Auth guard flow: no token → Login (stores pending invite token) → on auth success → `InviteAccept`

---

## Decisions

### D1: Navigation — JetBrains Navigation 3 (alpha)

**Chosen:** `org.jetbrains.androidx.navigation3:navigation3-ui:1.0.0-alpha05`

**Rationale:** Official CMP successor to AndroidX Navigation. Preserves type-safe serializable routes (same pattern as current `Screens.kt`). Migration surface is contained to `PlaybookNavGraph.kt` + `Screens.kt` (2 files).

**Rejected alternatives:**
- *Voyager*: stable but different paradigm; large API delta; locks to third-party
- *Decompose*: powerful but complex; YAGNI for MVP migration scope
- *AndroidX Navigation (keep Android, skip iOS nav sharing)*: defeats purpose; duplicates routing logic

**Risk:** Alpha stability. Mitigation: feature-freeze on navigation layer; upgrade to stable as soon as released. No fallback — Navigation 3 is the chosen path.

### D2: Image Loading — Coil 3.4.0

**Chosen:** `io.coil-kt.coil3:coil-compose:3.4.0` + `io.coil-kt.coil3:coil-network-ktor3`

**Rationale:** Only mature CMP-compatible image loading library. Required for iOS.

**Breaking changes to handle:**
- Group/package rename: `coil` → `coil3`
- Add `coil-network-ktor3` for network image loading
- `SingletonImageLoader.setSafe(context)` replaces `Coil.setImageLoader`
- `AsyncImage` API unchanged; `rememberAsyncImagePainter` API same
- `SizeResolver` defaults to `ORIGINAL` — no impact on current usage

### D3: Settings Storage — expect/actual with DataStore (Android) + NSUserDefaults (iOS)

**Chosen:** `expect class UserPreferences` with platform actuals: DataStore (`datastore-preferences:1.1.1`) on Android; `NSUserDefaults` on iOS.

**Rationale:** Implemented directly as expect/actual during Phase 0 scaffold. Both actuals are simple, self-contained, and use platform-native APIs. `multiplatform-settings` is retained in `commonMain` for potential future use but the `datastore` variant is not used.

**Deviation from original D3:** Original design specified `com.russhwolf:multiplatform-settings-datastore` to avoid expect/actual. Rejected during implementation — the extra indirection adds complexity without benefit given the minimal UserPreferences interface (3 keys: token, clubId, userId).

**Android continuity:** New `UserPreferences.android.kt` uses the same DataStore file name (`user_prefs`) and key names (`auth_token`, `club_id`) as the old `com.playbook.android.preferences.UserPreferences`. No migration required — same underlying storage.

### D4: ViewModel sharing — `lifecycle-viewmodel` in commonMain, Koin injection

**Chosen:** `androidx.lifecycle:lifecycle-viewmodel:2.8.7` in `composeApp/commonMain`. ViewModels are shared Kotlin classes. Koin `koinViewModel()` used in composables (CMP-compatible via `koin-compose-viewmodel`).

**iOS note:** `collectAsStateWithLifecycle` is Android-only. All `StateFlow` collection in `commonMain` uses `collectAsState()`. Lifecycle-aware collection on Android achieved via `lifecycle-runtime-compose` in `androidMain` only where strictly needed (currently: none in existing screens).

**Known issue (Phase 0):** `koinViewModel<T>()` crashes on iOS with Koin 4.0.0 — `LocalViewModelStoreOwner` is not found in the `ComposeUIViewController` context, causing an uncaught `IllegalStateException` on a background coroutine thread → SIGABRT. Workaround for Phase 0: `koinViewModel` call removed from `PlaybookApp`; the ViewModel module DSL import updated to `org.koin.core.module.dsl.viewModel`. **Must be resolved before CMP-017** — options: upgrade to Koin 4.2.0 (improved KMP ViewModel support) or provide `LocalViewModelStoreOwner` explicitly in `MainViewController`.

### D5: iOS Xcode Project — XcodeGen + XCFramework

**Chosen:** Xcode project generated from `iosApp/project.yml` via `xcodegen generate`. XCFramework built by Gradle and referenced via relative path in `project.yml`. `libsqlite3.tbd` linked as SDK dependency (required by SQLDelight NativeSqliteDriver).

**Rejected:** CocoaPods — adds toolchain dependency, `pod install` friction, and M1/M2 simulator build issues. Manual `.xcodeproj` editing — brittle, not reproducible.

**Key files:**
- `iosApp/project.yml` — source of truth for the Xcode project; commit this, not the generated `.xcodeproj` contents
- `iosApp/iosApp/iOSApp.swift` — `@main` SwiftUI App struct
- `iosApp/iosApp/ContentView.swift` — `UIViewControllerRepresentable` wrapping CMP

**Xcode project setup notes (Phase 0 learnings):**
- Required `sudo xcodebuild -license accept` before first framework build (Xcode license)
- Required `sudo xcodebuild -runFirstLaunch` to fix `DVTDownloads` framework version mismatch (macOS 26 beta)
- `codeSign: false` on the embedded XCFramework — simulator builds must not codesign frameworks
- Tested on: Xcode 26.3, iOS Simulator 26.2 SDK, iPhone 17 Pro simulator

**Build command:** `./gradlew :composeApp:assembleXCFramework` (run after any KMP change before building in Xcode).

**OneSignal iOS SDK** added via SPM manually in Xcode (NT-011).

### D6: Initial Auth State — No `runBlocking`

**Chosen:** `Screen.Splash` as initial back stack entry + `AuthViewModel` reading `UserPreferences` asynchronously via coroutine. See API / Interfaces → Initial Auth State section for implementation.

**Rejected:** `runBlocking` — deadlocks iOS main thread. Android accepted it historically but it's wrong there too (ANR risk under slow DataStore reads).

**AuthViewModel** lives in `composeApp/commonMain`, injected by Koin. It reads `UserPreferences` in `init { viewModelScope.launch { ... } }` and exposes `StateFlow<AuthState>`. `PlaybookApp` composable observes and drives navigation via `LaunchedEffect`.

---

## Pre-Migration Checklist

Before moving any screen to `composeApp/commonMain`, run:

```bash
grep -r "LocalContext" androidApp/src/
grep -r "LocalActivity" androidApp/src/
grep -r "rememberLauncherForActivityResult" androidApp/src/
grep -r "runBlocking" androidApp/src/
```

Expected results:
- `LocalContext`: any hit in a composable that will move to `commonMain` → refactor to expect/actual or move to `androidMain`. Coil 3 eliminates most `LocalContext` usages for image loading automatically.
- `LocalActivity`: must not appear in commonMain-bound composables
- `rememberLauncherForActivityResult`: none expected (push permission uses `PushPermissionRequester` expect/actual; no file pickers in scope)
- `runBlocking`: only in `PlaybookNavGraph.kt` — replaced by D6 pattern above

## Implementation Phases

Recommended order to reduce risk and enable incremental testing:

| Phase | Scope | Gate |
|-------|-------|------|
| 0 — Scaffold ✅ | Create `composeApp/` module, Gradle config, XcodeGen project, empty `PlaybookApp` composable, `AuthViewModel`, `Screen.Splash`. Android + iOS simulator both launch to blank screen. | Simulator builds green |
| 1 — Auth + Nav skeleton | Migrate `Screen.Login`, `PlaybookNavGraph`, `Screens.kt`, bottom nav, `AuthState` flow. Deep link wiring. | Login flow works on both platforms |
| 2 — Team Management | Migrate club/team/member/invite/player screens + VMs (7 screens) | All team features work on iOS sim |
| 3 — Events | Migrate eventlist, calendar, form, subgroupmgmt, eventdetail + VMs (5 screens) | All event features work on iOS sim |
| 4 — Attendance + Stats | Migrate attendance, absences, stats + VMs (3 screens) | All attendance features work on iOS sim |
| 5 — Notifications | Migrate notification inbox, settings, push permission + VMs (3 screens) | Push permission flow works on iOS |
| 6 — Cleanup | Delete all migrated code from `androidApp/`; verify `androidApp/` is thin shell only; run Android regression pass | Android behavior unchanged |

**22 ViewModels, ~80 composable files total.** Each phase results in a working, testable build on both platforms before proceeding.

## Risks

| Risk | Severity | Mitigation |
|------|----------|------------|
| Navigation 3 alpha breaks or API shifts | High | Pin exact version; don't upgrade mid-sprint. Fallback: Voyager |
| `runBlocking` in current NavGraph deadlocks iOS main thread | High | Replaced by D6 async `AuthViewModel` + `Screen.Splash` pattern — must be done in Phase 0 |
| Navigation 3 deep link handling not automatic (manual Intent → BackStack) | Medium | Implementation pattern documented in API / Interfaces section |
| `LocalContext` usages in composables block commonMain compilation | Medium | Run pre-migration grep checklist; refactor each hit before moving screen |
| Coil 3 migration has wider breaking changes than documented | Medium | Migrate Coil in an isolated branch; verify all image usages compile |
| Xcode project setup incorrect (missing framework embed, wrong target) | High | Use `xcodegen generate` from `iosApp/project.yml`; test on simulator immediately after Phase 0 scaffold ✅ resolved |
| XCFramework not rebuilt after KMP changes → stale iOS build | Medium | Always run `./gradlew :composeApp:assembleXCFramework` before Xcode builds after any Kotlin change |
| DataStore → multiplatform-settings: Android users lose persisted tokens on first launch after upgrade | Medium | Silent one-time migration on first launch (see D3); no re-login required |
| CMP iOS rendering performance vs native SwiftUI | Low | Acceptable for MVP; benchmark after first simulator run |
| `koinViewModel<T>()` crashes on iOS with Koin 4.0.0 | High | Removed from `PlaybookApp` for Phase 0. Must fix before CMP-017: upgrade Koin to 4.2.0 or provide `LocalViewModelStoreOwner` in `MainViewController` |
| macOS/Xcode beta SDK incompatibilities | Medium | `xcodebuild -runFirstLaunch` resolves framework mismatches; `xcodebuild -license accept` required after Xcode install |
| OneSignal iOS SDK manual steps (NT-011, NT-016) still required post-migration | Known | Documented; no change to status |
| Deep link URL scheme not registered on iOS | Medium | Add `playbook` URL scheme to `Info.plist` during Phase 0 iosApp setup |
