---
template: req
version: 0.1.0
gate: READY SET
---
# Requirements: Test Suite

## Goal
Implement a full automated test suite across every layer of the stack — mobile (Android + iOS), backend (Ktor), and super-admin (SvelteKit) — so that a fresh install, login, and every major flow works correctly, and the application is provably safe.

## Background
- All MVP features are implemented (team-management, event-scheduling, attendance-tracking, notifications, super-admin)
- CMP migration will unify Android + iOS under `composeApp/` — tests should target the shared layer
- Currently no automated tests exist in any module

## Dependency
**Blocked by: compose-multiplatform-migration** — test suite implemented after CMP migration is complete.

---

## In-Scope

### 1. Shared / Domain Layer (`:shared` — commonTest)
Unit tests for all business logic:
- Domain model validation (Team, Event, Attendance, Member, Abwesenheit, etc.)
- Repository fakes: verify contract without real network/db
- SQLDelight query tests (in-memory SQLite driver)
- Serialization round-trips (kotlinx-serialization)

### 2. Backend (`:backend`)

#### Unit Tests
- Service / use-case layer: isolated, no DB, all dependencies mocked
- Business rules: invite expiry, attendance auto-present job logic, abwesenheit backfill, J+S compliance checks
- Email template rendering
- JWT creation / validation logic
- Audit log formatting

#### Integration Tests
- All REST API endpoints tested end-to-end against a real test database:
  - Auth (register, login, refresh, logout)
  - Teams (CRUD, member invite, role changes)
  - Events (CRUD, subgroup assignment)
  - Attendance (confirm, decline, check-in, abwesenheit)
  - Notifications (trigger, preferences)
  - Super-admin routes (club/team overview, export, audit log)
- Database layer via Testcontainers (PostgreSQL)
- Flyway migration sanity check: schema applies cleanly from V1 → latest

#### Security Tests
- Auth middleware: unauthenticated requests → 401
- Role enforcement: player cannot access coach-only routes → 403
- SA routes require SA JWT → 403 for regular users
- JWT tampering / expired token → 401
- Input validation: malformed JSON, oversized payloads → 400 (no 500)
- SQL injection probes on all user-supplied query params
- OWASP Top 10 checklist coverage (documented per endpoint)

### 3. Android UI (`:composeApp` + `:androidApp`)
Compose UI tests (Robolectric + on-device):
- Login: valid credentials → home screen
- Login: invalid credentials → inline error
- Login: network offline → offline error state
- Team list: renders teams, taps to detail
- Event list / calendar: events render, form opens
- Attendance: confirm/decline updates badge state
- Absence: submit Begründung sheet
- Bottom nav: all tabs reachable
- Offline indicator: visible when network unavailable

### 4. iOS UI (`:composeApp` iOS target)
Post-CMP-migration — same critical smoke flows as Android:
- Login → home
- Team list navigation
- Event list navigation
- Attendance confirm/decline
- Bottom nav all tabs
Tooling TBD in tech spec.

### 5. Super-Admin Panel (`admin/` — SvelteKit)

#### Unit Tests (Vitest)
- Utility functions (date formatting, role label mapping, export helpers)
- Store logic / reactive state
- API client functions (mock fetch)

#### Component Tests (Vitest + Testing Library)
- Login form: renders, submits, shows error on failure
- Club list: renders rows, pagination, search filter
- Team detail: member list renders correctly
- Audit log: entries display with correct timestamps
- Export button: triggers download, shows loading state

#### E2E Tests (Playwright)
- SA login → dashboard loads
- Search for club → navigate to club detail
- Navigate to team → view members
- Trigger CSV export → file downloaded
- View audit log entries
- Logout → redirected to login

#### Security Tests
- Unauthenticated route access → redirected to login
- SA JWT required for all data endpoints (backend enforces, frontend verifies redirect)
- XSS: rendered user content is escaped (no raw innerHTML with user data)

### 6. End-to-End / Cross-Layer Smoke Tests
Manual or Maestro-automated critical user journey:
1. Register / login as coach (mobile)
2. Create a team
3. Invite a member
4. Member accepts invite
5. Coach creates an event
6. Member confirms attendance
7. Coach views attendance report
8. SA logs in, views the club and audit trail

---

## Out-of-Scope
- 100% line coverage (not a goal)
- Performance / load testing
- Accessibility testing (separate future spec)

## Acceptance Criteria
- [ ] `:shared:commonTest` passes — ≥ 80% domain layer coverage
- [ ] `:backend:test` passes all unit + integration tests; Testcontainers used for DB
- [ ] All security test cases documented and passing
- [ ] `:androidApp` Robolectric / connected UI tests pass all smoke flows
- [ ] iOS smoke tests pass on simulator
- [ ] `admin/` Vitest unit + component tests pass
- [ ] `admin/` Playwright E2E suite passes against local dev server
- [ ] CI pipeline runs all test suites on every PR
- [ ] Zero flaky tests at merge

## Dependencies
- **Blocked by:** compose-multiplatform-migration
