# STATE.md — Playbook

## Current State
- **Active phase:** Phase 1 — completing iOS wiring
- **Mode:** YOLO
- **Last updated:** 2026-03-16

## Phase Status

| Phase | Status | Started | Completed |
|---|---|---|---|
| 1 — Foundation + Auth | 🔶 In progress | 2026-03-11 | — |
| 2 — Team Management | 🔶 In progress | 2026-03-11 | — |
| 3 — Event Scheduling | 🔲 Not started | — | — |
| 4 — Attendance Tracking | 🔲 Not started | — | — |
| 5 — Notifications | 🔲 Not started | — | — |
| 6 — Super Admin | 🔲 Not started | — | — |

## What's Actually Done

### Phase 1 — Foundation + Auth
- ✅ KMP monorepo scaffolded (shared, composeApp, androidApp, iosApp, server, admin)
- ✅ Ktor server: DB (PostgreSQL + Flyway), JWT auth, register/login/logout
- ✅ Role system in DB (Coach, Player, ClubManager, SuperAdmin)
- ✅ Android app builds and runs
- ✅ Shared KMP code: ViewModels, screens, navigation (Nav3 CMP), Koin DI
- ✅ Auth screens: Login, Register (Android working)
- ✅ CI/CD: Test Suite + Deploy Android pipelines, iOS XCTest smoke tests
- ✅ iOS scaffold: Tuist project, KMP framework linked, smoke tests run in CI
- ✅ iOS UIKit lifecycle wired (AppDelegate + UIWindowSceneDelegate)
- ✅ NavigationEventDispatcher crash fixed (JetBrains rc01 substitution)
- ⏳ Waiting for Miggi to verify in simulator

### Phase 2 — Team Management
- ✅ Backend: clubs, teams, invite system, role assignments, sub-groups (DB + API)
- ✅ Flyway migrations V1–V6
- ✅ Android UI: EmptyState, ClubSetup, TeamRoster, Invite screens + ViewModels
- ❌ Not fully tested end-to-end on device

## Open Items (before closing Phase 1)
1. ✅ Wire CMP UI into `iosApp/iosApp/iOSApp.swift` — UIKit lifecycle
2. ✅ Fix NavigationEventDispatcher crash (2026-03-16)
3. Add SPM deps to `iosApp/Project.swift` as needed (Miggi to add if needed)
4. Verify iOS app runs in simulator with auth flow working (Miggi, needs Mac)

## Notes
- `failed example project/` can be deleted once iOS is running
- CI budget exhausted until ~2026-04-01 — work on feature branches, only merge to main when ready
- Branching: `feat/phase-3-planning` is current working branch
