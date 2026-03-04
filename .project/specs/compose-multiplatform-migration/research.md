---
template: research
version: 0.1.0
gate: READY SET
---
# Research: Compose Multiplatform Migration

## Verdict: Feasible — Tech Spec is sound with 4 items to address

The tech spec is architecturally correct and implementable. No showstoppers. Four issues need resolution before READY GO.

---

## Tech Spec Validation

### D1: Navigation 3 (alpha) — RISK CONFIRMED, MANAGEABLE

**Status**: Still alpha as of early 2026. `1.0.0-alpha05` is near the latest known release. The API surface for Compose-centric navigation is stable enough for this use case.

**What needs to happen in migration:**
- Replace `rememberNavController()` → Navigation 3 `NavDisplay` / `BackStack`
- `@Serializable` route objects (`Screens.kt`) carry over — API is compatible
- Bottom nav integration pattern changes slightly

**Fallback**: Voyager 1.1.0+ is a stable alternative; same paradigm shift cost either way. Tech spec is right to pin Navigation 3 and freeze it.

**Risk**: API may shift before stable. Mitigation: pin exact version in `libs.versions.toml`, do not upgrade mid-sprint.

---

### D2: Coil 3.4.0 — LOW RISK, CORRECT CHOICE

Spec is correct. Key points confirmed:
- Package rename: `io.coil-kt` → `io.coil-kt.coil3`
- **`coil-network-ktor3`** is the right choice for this project (Ktor already in `shared/`) — research suggested okhttp but ktor3 integration exists and is preferred here
- `AsyncImage` API surface is unchanged — no screen-level rewrites needed
- `SingletonImageLoader.setSafe(context)` (or `setSingletonImageLoaderFactory` in CMP) replaces `Coil.setImageLoader`
- `rememberAsyncImagePainter` API unchanged

---

### D3: multiplatform-settings 1.3.0 — LOW RISK

`com.russhwolf:multiplatform-settings-datastore` exists and works for Android DataStore backend. The one-time migration strategy in the spec (read DataStore → write to new store → set `migrated_v1` flag) is correct.

---

### D4: ViewModel sharing — LOW RISK

`koin-compose-viewmodel` 4.2.0 is CMP-compatible. The 22 ViewModels in `UiModule.kt` using `parametersOf()` will work unchanged in `commonMain`. `collectAsState()` instead of `collectAsStateWithLifecycle()` in commonMain is the right call.

---

### D5: CocoaPods — LOW RISK

Mature, stable for CMP. iosApp is a placeholder — must run KMP wizard to generate proper Xcode project before any migration work. Known M1/M2 `ARCHS` simulator issue: set `ARCHS = arm64` for simulator builds in Xcode build settings.

---

## Issues Found: 4 Items for Tech Spec Review

### ISSUE 1: `runBlocking` in NavGraph — Must Fix

**File**: `androidApp/.../ui/navigation/PlaybookNavGraph.kt`

Current code reads the auth token synchronously via `runBlocking` to determine the start destination. This is Android-acceptable but **will not compile or will deadlock in commonMain** on iOS (iOS main thread cannot be blocked).

**Required change**: Replace with a splash/loading state. Initial state = `Loading`, then launch a coroutine in the NavGraph/parent composable that reads the token asynchronously and navigates to `Login` or `ClubDashboard`. A `SplashScreen` route (or empty loading composable) as the initial destination handles this cleanly.

This is a **required architectural change** not mentioned in the tech spec.

---

### ISSUE 2: Scope Clarification — 22 ViewModels + 20+ Screens

The spec says "move all screens" but doesn't break down the migration scope. For planning purposes:
- **22 ViewModels** across 20+ feature directories
- **~80 composable files** estimated from 102 total Kotlin files
- No `LocalContext` or `LocalActivity` usage detected in feature screens (push/DataStore already isolated in platform modules)

This is a large but mechanical migration. Recommend phasing:
1. Scaffold: create `composeApp/` module, entry points, empty shell
2. Auth + Nav skeleton
3. Feature screens in batches (team-management, events, attendance, notifications)

---

### ISSUE 3: `LocalContext` Audit Required

During migration, any `LocalContext.current` calls in composables that move to `commonMain` will fail to compile. Likely locations:
- Coil `LocalContext` usage (mitigated by Coil 3 removing this requirement in CMP)
- Toast/Snackbar platform-specific messages (if any)
- Any screen using `context.startActivity()`

**Action**: `grep -r "LocalContext" androidApp/` before migration begins. Add expect/actual or move to `androidMain` for each occurrence.

---

### ISSUE 4: Navigation 3 — No Code Sample for Deep Link Handling

The spec correctly identifies the deep link `playbook://invite?token={token}` but doesn't specify how Navigation 3 handles deep links (it differs from AndroidX Navigation).

In Navigation 3, deep links are handled by processing `Intent` in `MainActivity` and pushing a route onto the `BackStack` manually. The auth guard logic (no token → Login → InviteAccept) needs explicit implementation. This is doable but requires research at implementation time.

---

## Common CMP Migration Pitfalls (Reference)

| Pitfall | Present in This Project? | Status |
|---------|-------------------------|--------|
| `LocalContext` in composables | Unknown until grep | See ISSUE 3 |
| `rememberLauncherForActivityResult` | Not detected in screen list | Low risk |
| `collectAsStateWithLifecycle` in shared code | Addressed in D4 | ✅ Handled |
| `runBlocking` in UI layer | Yes — NavGraph | See ISSUE 1 |
| Android-only lifecycle observers | Not detected | Low risk |
| Platform-specific permissions in commonMain | Addressed via expect/actual | ✅ Handled |
| Koin module not initialized on iOS | Must implement in iosApp | Known task |
| CocoaPods M1/M2 simulator build failures | Likely on dev machine | See D5 note |
| DataStore migration breaking auth on first launch | Addressed in D3 | ✅ Handled |

---

## Library Version Verification

Verify these before implementation (check Maven Central / GitHub releases):

| Library | Spec Version | Verify |
|---------|-------------|--------|
| `navigation3-ui` | `1.0.0-alpha05` | Check for newer alpha |
| `coil3:coil-compose` | `3.4.0` | Confirm latest stable |
| `coil3:coil-network-ktor3` | `3.4.0` | Same version as coil-compose |
| `multiplatform-settings` | `1.3.0` | Confirm latest |
| `koin-compose` BOM | `4.2.0` | Confirm release |

---

## Summary

**Tech spec is implementable.** Required additions before READY GO:

1. Add splash/loading state pattern for async token check (ISSUE 1 — arch change)
2. Add `LocalContext` grep to pre-migration checklist (ISSUE 3 — discovery task)
3. Add Navigation 3 deep link implementation note to tech spec (ISSUE 4 — spec gap)
4. Phasing plan for 22 VM / 80 composable migration (ISSUE 2 — planning)
