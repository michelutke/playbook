---
template: overview
version: 0.1.0
---
# Project Overview: Playbook

## Description
Playbook is a cross-platform team appointment and attendance management app for sports clubs. It targets clubs participating in official Jugend und Sport (J+S) programs that require Anwesenheitskontrolle. Think Spielerplus.de, built with Kotlin Multiplatform + Compose Multiplatform.

## Repository Structure (Monorepo — ADR-002)

```
/
├── shared/       — KMP shared domain, data, business logic, SQLDelight cache
├── composeApp/   — KMP Compose Multiplatform UI (commonMain + androidMain + iosMain)
├── androidApp/   — Android thin shell (MainActivity, Koin init, push wiring only)
├── iosApp/       — iOS entry point (Swift AppDelegate → ComposeUIViewController)
├── server/       — Ktor JVM server + Flyway migrations
└── admin/        — SvelteKit SuperAdmin panel
```

## Tech Stack
- Kotlin 2.3.10, Compose Multiplatform 1.10.1, AGP 8.13.2, Gradle 8.13
- Koin 4.1.0, Lifecycle 2.9.1 (expect/actual `kmpViewModel` — iOS bypasses KoinViewModelFactory)
- Ktor 3.1.0 (backend), kotlinx-datetime 0.6.0 (forced — 0.7.x causes Kotlin/Native IR crash)
- Navigation3 1.0.0-alpha06 + Coil3 3.4.0

## Current Status

**Phase:** Feature-complete (MVP features shipped); CMP migration done

| Feature | Status | Tasks |
|---|---|---|
| team-management | ✅ DONE | 65/65 |
| event-scheduling | ✅ DONE | 36/36 |
| attendance-tracking | ✅ DONE | 35/35 |
| notifications | ✅ DONE (2 manual iOS steps) | 58/58 |
| super-admin | ✅ DONE | 52/52 |
| compose-multiplatform-migration | ✅ DONE | CMP-001–051 |
| test-suite | 🔲 Pending | READY SET |

### Remaining Manual Steps
- NT-011: Add Push Notification capability in Xcode
- NT-016: Set OneSignal App ID env var in Xcode scheme
- CMP-049: Configure OneSignal in Xcode (already wired in Kotlin)
- CMP-050: Add Background Modes capability in Xcode

## Open ADRs
- **ADR-001 (main app web)**: main app web client tech deferred — SA panel uses SvelteKit, main app web TBD

## Archived Specs
Completed feature specs archived at `.project/specs/_archive/`:
- `team-management/`
- `event-scheduling/`
- `attendance-tracking/`
- `notifications/`
- `super-admin/`

Each archive contains a `handover.md` with decisions, key files, migrations, and known limitations.
