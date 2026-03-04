---
template: req
version: 0.1.0
gate: READY SET
---
# Requirements: Compose Multiplatform Migration

## Goal
Move all UI code from `androidApp/` into a shared `composeApp/` module so the same Compose screens and components run on both Android and iOS. The `iosApp/` must be properly configured as a CMP host.

## Background
- `androidApp/` currently holds all Compose screens, navigation, DI bootstrapping, and platform integrations
- `iosApp/` is a placeholder skeleton — not yet a functioning CMP host
- `shared/` contains domain, data layer, SQLDelight cache (already KMP-ready)

## In-Scope

### New `composeApp/` Shared UI Module
- Create `composeApp/` KMP module targeting Android + iOS
- Move **all** existing `androidApp/` screens, components, and navigation into `composeApp/`
  - Authentication screens
  - Team management screens
  - Event scheduling screens (list, calendar, form, subgroup management)
  - Attendance tracking screens (list, absences, stats)
  - Notification permission + settings screens
  - Bottom navigation bar
  - Shared UI components (StatusBadge, OfflineIndicator, EventTypeIndicator, etc.)
- `androidApp/` becomes a thin shell: MainActivity + Android-specific DI init only
- `iosApp/` becomes a thin shell: SwiftUI entry point wrapping CMP + iOS-specific DI init only

### Library Compatibility Research (required before tech spec)
During the tech/design phase, each library currently used in `androidApp/` must be evaluated:
- Confirm CMP-compatible alternative exists **or** platform-specific expect/actual needed
- Libraries to evaluate (non-exhaustive):
  - Navigation (currently Android Navigation Compose)
  - Image loading (Coil, Glide, or similar)
  - OneSignal SDK (Android-only → needs iOS SDK wiring)
  - Any other androidApp-only dependencies
- Output: compatibility matrix in tech spec

### `iosApp/` Proper Setup
- Configure `iosApp/` Xcode project to embed the CMP framework produced by `composeApp/`
- Minimum iOS target: iOS 16 (or align with CMP minimum)
- SwiftUI `ContentView` delegates entirely to the CMP root composable
- CocoaPods or SPM integration for KMP/CMP framework
- OneSignal iOS SDK added via SPM (NT-011)
- Background Modes → Remote notifications enabled (NT-016)
- OneSignal App ID injected via Info.plist / env

### Feature Parity
All features working on Android must work identically on iOS post-migration:
- Login / auth flow
- Team management
- Event scheduling
- Attendance tracking
- Push notifications
- Offline mode (SQLDelight cache)

## Out-of-Scope
- New features
- Web client (ADR-001, deferred)
- Visual redesign

## Acceptance Criteria
- [ ] `composeApp/` module builds for both Android and iOS targets
- [ ] `androidApp/` contains no screen/component Compose code (only entry point)
- [ ] `iosApp/` Xcode project builds and runs on simulator without errors
- [ ] All five feature areas fully functional on iOS simulator
- [ ] Android behavior unchanged after refactor
- [ ] Push notifications work on real iOS device (after NT-011/NT-016)
- [ ] Offline cache works on iOS

## Dependencies
- None (this unblocks: **test-suite**)

## Carried Over Tasks
- NT-011: Add OneSignal XCFramework via SPM to Xcode project
- NT-016: Enable Background Modes → Remote notifications in Xcode
