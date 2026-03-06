---
template: plan
version: 0.1.0
gate: READY GO
---
# Plan: Test Suite

## Overview

Full automated test suite across every layer. Split into two parallel tracks: Phase 1 (CMP-independent — can start now) and Phase 2 (blocked by CMP migration). Phases 1a–1d are themselves parallelisable.

**Dependency:** `:composeApp` + iOS tests blocked by `compose-multiplatform-migration`. Backend, shared, and admin tests are fully independent.

---

## Phase 1a — Backend Integration + Security Tests
**Can start:** immediately (no CMP dependency)
**Prerequisite:** Docker available locally and in CI

### Setup
1. Add to `backend/build.gradle.kts`:
   ```kotlin
   testImplementation("io.ktor:ktor-server-tests-jvm:3.1.0")
   testImplementation("io.ktor:ktor-client-content-negotiation:3.1.0")
   testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
   testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.3")
   testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.3")
   testImplementation("org.testcontainers:testcontainers:1.20.2")
   testImplementation("org.testcontainers:junit-jupiter:1.20.2")
   testImplementation("org.testcontainers:postgresql:1.20.2")
   testImplementation("io.insert-koin:koin-test:4.0.0")
   tasks.withType<Test> { useJUnitPlatform() }
   ```
2. Create `IntegrationTestBase.kt` with Testcontainers PostgreSQL + `testApp {}` helper + `bearerToken()` / `saToken()` helpers + `resetSchema()` (see tech spec pattern)
3. Create `Fixtures.kt` with test UUIDs + `seedCoachAndTeam(db)` helper

### Route Integration Tests (one test class per route group)
Each class covers: happy path, 401 unauthenticated, 403 role violation, 404 not found, 400 malformed input

| Test Class | Routes covered |
|-----------|---------------|
| `AuthRoutesTest` | POST /register, POST /login, POST /refresh, POST /logout |
| `ClubRoutesTest` | GET/POST/PUT /clubs, /clubs/:id |
| `TeamRoutesTest` | GET/POST/PUT/DELETE /teams, /teams/:id |
| `MemberRoutesTest` | GET /teams/:id/members, DELETE membership |
| `InviteRoutesTest` | POST /invites, GET /invites/:token, POST /invites/:token/accept |
| `CoachLinkRoutesTest` | POST /coach-links, POST /coach-links/:token/accept |
| `EventRoutesTest` | GET/POST/PUT/DELETE /events, subgroup assignment |
| `AttendanceRoutesTest` | POST confirm/decline, GET by event, check-in |
| `AbwesenheitRoutesTest` | POST /abwesenheit, GET by user/team |
| `NotificationRoutesTest` | GET /notifications, POST preferences |
| `SuperAdminRoutesTest` | GET /sa/clubs, /sa/teams, export, audit log |

### Security Tests (`SecurityTest.kt`)
- Unauthenticated → 401 for all protected routes
- Player → 403 on coach-only routes (event create, team edit, invite generate)
- Regular JWT → 403 on SA routes
- Expired JWT → 401
- Tampered JWT (invalid signature) → 401
- Malformed JSON body → 400 (not 500)
- Oversized payload → 400 or 413
- SQL injection probes on query params (team name search, member filter)
- OWASP checklist: document per endpoint in test file comments

### Flyway smoke
- Verify `resetSchema()` applies V1 → latest cleanly (runs implicitly via each test class)

**Exit gate:** `./gradlew :backend:test` green; all route classes pass; security assertions documented

---

## Phase 1b — Backend Pure Logic Unit Tests
**Can start:** immediately (no infrastructure needed)

### Test classes (no `@Testcontainers`)
| Test Class | Subject |
|-----------|---------|
| `TokenGeneratorTest` | `generateToken()` — length, charset, uniqueness |
| `InviteEmailTest` | HTML string output for given inputs |
| `CoachLinkEmailTest` | HTML string output |
| `TeamApprovalEmailTest` | HTML string output |
| `ManagerInviteEmailTest` | HTML string output |
| `JwtTokenTest` | JWT create → claims extraction, issuer/audience/expiry |

**Exit gate:** All unit tests pass with no infrastructure

---

## Phase 1c — Shared KMP Tests
**Can start:** immediately

### Setup
1. Add to `shared/build.gradle.kts`:
   - Plugins: `id("com.google.devtools.ksp") version "2.1.10-1.0.29"` + `id("io.kotest.multiplatform") version "5.10.0"`
   - `commonTest`: `kotlin("test")`, `kotlinx-coroutines-test:1.9.0`, `kotest-framework-engine:5.10.0`, `kotest-assertions-core:5.10.0`
   - `jvmTest`: `sqldelight:sqlite-driver:2.0.2`, `junit-jupiter:5.10.3`
   - KSP: `add("kspCommonMainMetadata", "io.kotest:kotest-framework-multiplatform-plugin-gradle:5.10.0")`

### commonTest (Kotest StringSpec / FunSpec)
| Test Class | Subject |
|-----------|---------|
| `TeamDomainTest` | Team model validation, membership rules |
| `EventDomainTest` | Event model, date validation, subgroup assignment |
| `AttendanceDomainTest` | Status transitions (confirm, decline), business rules |
| `AbwesenheitDomainTest` | Abwesenheit creation, period overlap |
| `MemberDomainTest` | Role validation, invite status transitions |
| `SerializationRoundTripTest` | All domain models: serialize → deserialize → equals |
| `RepositoryFakeTest` | In-memory fake repos satisfy the repository interface contract |

Each domain test: valid construction, boundary values, invalid state rejection.

### jvmTest (JUnit 5 + JDBC SQLite)
| Test Class | Subject |
|-----------|---------|
| `AttendanceQueriesTest` | insert, selectByEventId, selectByUserId, update status |
| `EventQueriesTest` | insert, selectByTeamId, selectById, delete |
| `NotificationQueriesTest` | insert, selectUnread, markRead |

Pattern: `@BeforeEach` creates in-memory driver + `PlaybookDatabase.Schema.create(driver)`; `@AfterEach` closes driver.

**Exit gate:** `./gradlew :shared:allTests` green

---

## Phase 1d — Admin Panel Tests
**Can start:** immediately

### Setup (`admin/`)
1. Install: `vitest@^2.1.0`, `@vitest/ui`, `@testing-library/svelte@^5.2.0`, `@testing-library/jest-dom@^6.4.0`, `@playwright/test@^1.41.0`, `jsdom@^25.0.0`
2. Add `test` block to `vite.config.ts` (`environment: 'jsdom'`, `globals: true`, `setupFiles: ['./src/test/setup.ts']`)
3. Add npm scripts: `test`, `test:ui`, `test:e2e`

### Vitest Unit Tests
| Test File | Subject |
|-----------|---------|
| `dateUtils.test.ts` | Date formatting, relative time, locale |
| `roleLabel.test.ts` | Role label mapping (coach/player/SA) |
| `exportHelpers.test.ts` | CSV/export string builders |
| `apiClient.test.ts` | API client functions with mocked `fetch`; auth header injection, error handling |
| `authStore.test.ts` | SA auth store: login sets token, logout clears token |

### Vitest Component Tests (Testing Library)
| Test File | Component |
|-----------|-----------|
| `LoginForm.test.ts` | Renders fields; submit with valid creds; shows error on 401 |
| `ClubList.test.ts` | Renders club rows; search filter; pagination |
| `TeamDetail.test.ts` | Member list renders; role chips correct |
| `AuditLog.test.ts` | Entries display with timestamps; correct actor names |
| `ExportButton.test.ts` | Click triggers download; loading state shown |

### Playwright E2E (`admin/e2e/`)
| Test File | Flow |
|-----------|------|
| `login.spec.ts` | SA login → dashboard |
| `clubs.spec.ts` | Search club → navigate to detail → view teams |
| `export.spec.ts` | Trigger CSV export → file downloaded |
| `auditLog.spec.ts` | View audit log entries |
| `logout.spec.ts` | Logout → redirected to login |
| `security.spec.ts` | Unauthenticated route → redirected to login; XSS: rendered user content escaped |

**Exit gate:** `npm run test` and `npm run test:e2e` green

---

## Phase 2a — Android UI Tests (Robolectric)
**Blocked by:** `compose-multiplatform-migration` Phase 0–6 complete

### Setup
Add to `composeApp/build.gradle.kts`:
```kotlin
android {
    testOptions { unitTests { isIncludeAndroidResources = true } }
}
// androidUnitTest dependencies: robolectric:4.12.1, compose-ui-test-junit4:1.7.1, compose-ui-test-manifest
```

### Test classes (`composeApp/src/androidUnitTest/`)
Each test: `@RunWith(AndroidJUnit4::class)` + `@Config(sdk = [34])`

| Test Class | Smoke Flows |
|-----------|------------|
| `LoginScreenTest` | Valid submit → success callback; empty submit → inline error; loading state shown |
| `ClubDashboardTest` | Teams render; empty state shown; FAB visible to coach |
| `TeamDetailTest` | Members list renders; tapping member navigates |
| `EventListTest` | Events render; calendar tab accessible; form opens |
| `AttendanceListTest` | Confirm → badge updates to CONFIRMED; Decline → DECLINED |
| `BottomNavTest` | All tabs reachable; notification badge shows unread count |
| `OfflineIndicatorTest` | Banner visible when `NetworkMonitor` offline state injected |
| `AbsenceSheetTest` | Open BegrundungSheet; submit with reason |

**Exit gate:** `./gradlew :composeApp:testDebugUnitTest` green on CI

---

## Phase 2b — Shared Composable Tests (commonTest)
**Blocked by:** `compose-multiplatform-migration` complete; `composeApp/` module exists

### Setup
`composeApp/commonTest` dependencies: `ui-test-junit4:1.7.1` (pinned — do not upgrade independently)

### Test classes (experimental `runComposeUiTest`)
Scope: composable rendering + state logic, not full flows

| Test Class | Composables |
|-----------|------------|
| `StatusBadgeTest` | CONFIRMED/DECLINED/PENDING renders correct label + color |
| `EventTypeIndicatorTest` | Training/Match/Other renders correct icon |
| `OfflineIndicatorTest` | Shown/hidden based on state prop |
| `PlaybookBottomBarTest` | Active tab highlighted; badge renders |
| `AuthStateNavigationTest` | `AuthState.Unauthenticated` → Login route; `Authenticated` → Dashboard route |

**Exit gate:** `./gradlew :composeApp:commonTest` green (JVM runner)

---

## Phase 2c — iOS Smoke Tests (XCTest)
**Blocked by:** `compose-multiplatform-migration` complete + iosApp Xcode project built

### Prerequisite
All interactive composable elements have `.testTag("accessibility_id")` set during CMP migration. CMP maps `testTag` → `accessibilityIdentifier` on iOS.

### XCTest suite (`iosApp/iosAppUITests/`)

| Test Class | Flows |
|-----------|-------|
| `LoginFlowTests` | Valid credentials → "Meine Teams" appears; invalid → error shown |
| `TeamListTests` | Team row visible; tap → TeamDetail screen |
| `EventListTests` | Event list screen; tap event → detail |
| `AttendanceTests` | Confirm attendance → badge updates |
| `BottomNavTests` | All bottom nav tabs reachable |

Pattern: `XCUIApplication().launch()` + `waitForExistence(timeout: 5)` assertions (see tech spec Swift example)

**Exit gate:** All XCTest flows pass on iOS 16 simulator

---

## Phase 3 — Maestro Cross-Layer E2E
**Blocked by:** Android emulator + iOS simulator both functional (after Phase 2)

### Flows (`maestro/flows/`)

| Flow File | Journey |
|-----------|---------|
| `coach-register.yaml` | Register coach → create club → create team → ClubDashboard |
| `member-invite.yaml` | Coach invites member → member accepts via deep link → team roster updated |
| `event-attendance.yaml` | Coach creates event → member confirms → coach views attendance report |
| `sa-audit.yaml` | SA logs in → views club → views audit log |
| `full-journey.yaml` | Composite of all above flows |

Each flow uses `testTag`-based element IDs consistent with Phase 2a/2c test tags.

**Exit gate:** All Maestro flows pass on Android emulator + iOS simulator

---

## Phase 4 — CI Pipeline
**Blocked by:** All test phases complete

### CI jobs (add to `.gitlab-ci.yml` or equivalent)

| Job | Command | Runner requirement |
|-----|---------|-------------------|
| `test:backend` | `./gradlew :backend:test` | Docker (Testcontainers) |
| `test:shared` | `./gradlew :shared:allTests` | JVM |
| `test:android` | `./gradlew :composeApp:testDebugUnitTest` | JVM |
| `test:admin` | `npm run test && npm run test:e2e` | Node + browser |
| `test:ios` | Xcode CLI `xcodebuild test` | macOS runner |

### Config
- `~/.testcontainers.properties`: `testcontainers.reuse.enable=true` (local only — not CI)
- Parallel jobs for 1a, 1b, 1c, 1d — no dependency between them
- Phase 2+ jobs only on branches that completed CMP migration

**Exit gate:** All CI jobs green on every PR; zero flaky tests

---

## Risks (watch during implementation)

| Risk | Watch for |
|------|-----------|
| Kotest KSP version mismatch | Pin `ksp = "2.1.10-1.0.29"`; update together with Kotlin |
| Robolectric silently not rendering Compose | First check `isIncludeAndroidResources = true` if tests pass but no nodes found |
| `runComposeUiTest` API breaks on CMP upgrade | Pin CMP 1.7.1; flag any upgrade in composeApp |
| Flyway `clean()` hitting non-test DB | `require(jdbcUrl.contains("localhost"))` guard in `resetSchema()` |
| Testcontainers cold start in CI | Docker pull caching + `ryuk` reuse where supported |
| XCTest `testTag` → `accessibilityIdentifier` missing | Add `testTag` during CMP migration Phase 0–6, not after |
| Backend has no service layer for unit testing | Scope is pure-function unit tests only (TokenGenerator, email, JWT) — routes covered by integration tests |
