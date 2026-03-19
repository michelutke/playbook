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
| 3 — Event Scheduling | 🔲 Not started | — | — |
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

## Notes
- CI budget exhausted until ~2026-04-01 — work on feature branches, only merge to main when ready
