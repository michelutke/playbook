---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
status: unknown
last_updated: "2026-03-19T11:02:11.975Z"
progress:
  total_phases: 3
  completed_phases: 0
  total_plans: 8
  completed_plans: 2
---

# STATE.md — Playbook

## Current State
- **Active phase:** Phase 3 — Event Scheduling
- **Mode:** YOLO
- **Last updated:** 2026-03-19

## Phase Status

| Phase | Status | Started | Completed |
|---|---|---|---|
| 1 — Foundation + Auth | ✅ Done | 2026-03-11 | 2026-03-19 |
| 2 — Team Management | ✅ Done | 2026-03-11 | 2026-03-19 |
| 3 — Event Scheduling | 🔄 In progress | 2026-03-19 | — |
| 4 — Attendance Tracking | 🔲 Not started | — | — |
| 5 — Notifications | 🔲 Not started | — | — |
| 6 — Super Admin | 🔲 Not started | — | — |

## What's Actually Done

### Phase 1 — Foundation + Auth ✅
- ✅ KMP monorepo scaffolded (shared, composeApp, androidApp, iosApp, server, admin)
- ✅ Ktor server: DB (PostgreSQL + Flyway), JWT auth, register/login/logout
- ✅ Role system in DB (Coach, Player, ClubManager, SuperAdmin)
- ✅ Android + iOS apps build and run, login flow verified in simulator
- ✅ Shared KMP code: ViewModels, screens, navigation (Nav3 CMP), Koin DI
- ✅ Auth screens: Login, Register working on Android + iOS
- ✅ CI/CD: Test Suite + Deploy Android pipelines, iOS XCTest smoke tests

### Phase 2 — Team Management ✅
- ✅ Backend: clubs, teams, invite system, role assignments, sub-groups (DB + API)
- ✅ Flyway migrations V1–V6
- ✅ Android UI: EmptyState, ClubSetup, TeamRoster, Invite screens + ViewModels
- ✅ UI + E2E tests for all team management flows
- ✅ GitHub Actions: CI build check + Updraft deploy workflow
- ⚠️ Updraft deploy needs `UPDRAFT_APP_ID` + `UPDRAFT_API_KEY` secrets added to GitHub

### Phase 3 — Event Scheduling (in progress)
- ✅ 03-00: Wave 0 test stubs — 9 stub files across shared/server/composeApp
- ✅ 03-01: Event DB foundation — V7 migration + Exposed tables + EventRepository (interface + impl)

## Decisions
- Used `kotlin.test.@Ignore` for shared/server stubs (consistent with existing test convention)
- Used `org.junit.@Ignore` for androidTest stubs (JUnit 4 standard for Android instrumented tests)
- Created `composeApp/src/androidTest/` with `androidInstrumentedTest` source set (VALIDATION.md targets `connectedAndroidTest`)
- [Phase 03-event-scheduling]: Used enumerationByName for EventType/EventStatus/PatternType to store as TEXT matching V7 CHECK constraints
- [Phase 03-event-scheduling]: Used array<Short> for weekdays column — confirmed supported in Exposed 0.54.0 via resolveColumnType

## Notes
- CI budget exhausted until ~2026-04-01 — work on feature branches, only merge to main when ready
- Last session: 2026-03-19 — Completed 03-01-PLAN.md (Event DB Foundation)
