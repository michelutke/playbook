---
template: tasks
version: 0.1.0
gate: READY GO
---
# Tasks: Test Suite

## Phase 1a — Backend Integration + Security Tests ✅ DONE

| ID | Task | Deps | Status |
|---|---|---|---|
| TS-001 | Add test deps to `server/build.gradle.kts`: Ktor server tests, JUnit 5, Testcontainers (core + junit-jupiter + postgresql), Koin test; `useJUnitPlatform()` | — | ✅ |
| TS-002 | Create `IntegrationTestBase.kt`: `@Testcontainers` PostgreSQL container, `testApp {}` helper with `MapApplicationConfig`, `bearerToken()` / `saToken()` / `expiredToken()` helpers, `resetSchema()` with Flyway `clean()+migrate()` + `Database.connect()` | TS-001 | ✅ |
| TS-003 | Create `Fixtures.kt`: test UUIDs (`TEST_COACH_ID`, `TEST_PLAYER_ID`, `TEST_CLUB_ID`, `TEST_TEAM_ID`, `TEST_SA_ID`, `TEST_MANAGER_ID`); `seedCoachAndTeam(db)` helper | TS-002 | ✅ |
| TS-004 | `AuthRoutesTest`: happy path + 400/401 for POST /register, /login, /refresh, /logout | TS-003 | ✅ |
| TS-005 | `ClubRoutesTest`: GET/POST/PATCH /clubs, /clubs/:id — happy path + 401 + 403 + 404 | TS-003 | ✅ |
| TS-006 | `TeamRoutesTest`: GET/POST/PATCH /teams, /clubs/:id/teams — happy path + 401 + 403 | TS-003 | ✅ |
| TS-007 | `MemberRoutesTest`: GET /teams/:id/members, DELETE membership, profile update | TS-003 | ✅ |
| TS-008 | `InviteRoutesTest`: POST /invites, GET /invites/:token (200 + 410), accept | TS-003 | ✅ |
| TS-009 | `CoachLinkRoutesTest`: POST /coach-links, resolve (public), rotate | TS-003 | ✅ |
| TS-010 | `EventRoutesTest`: GET/POST/PATCH /events; my-events; subgroup assignment | TS-003 | ✅ |
| TS-011 | `AttendanceRoutesTest`: POST confirm/decline, GET by event, check-in toggle | TS-003 | ✅ |
| TS-012 | `AbwesenheitRoutesTest`: POST /abwesenheit, GET by user, GET by team | TS-003 | ✅ |
| TS-013 | `NotificationRoutesTest`: GET /notifications (unread), POST preferences, push token | TS-003 | ✅ |
| TS-014 | `SuperAdminRoutesTest`: GET /sa/stats, /sa/clubs; POST /sa/clubs; SA token required | TS-003 | ✅ |
| TS-015 | `SecurityTest`: unauthenticated 401; player 403 on coach-only; regular JWT 401 on SA routes; expired JWT 401; tampered JWT 401; malformed JSON 400; SQL injection probes | TS-002 | ✅ |

**Production bugs caught:**
- `MembershipRepositoryImpl`: ambiguous Exposed join when table has 2 FKs to same table → explicit `join(UsersTable, JoinType.INNER, userId, id)`
- `AuditPlugin`: response body silently dropped (missing `transformBody` in `onCallRespond`)
- `V28` migration: column type mismatches (`weekdays SMALLINT[]→TEXT`, `payload JSONB→TEXT`)
- Background jobs: coroutine scopes not cancelled on app stop → test leaks; added `monitor.subscribe(ApplicationStopping) { scope.cancel() }`
- `SecurityTest` tampered JWT: last base64url char is padding bits — changed to tamper first signature char
- HikariCP pool accumulation: `minimumIdle=1` prevents 42+ pools × 5 idle conns exceeding postgres limit

## Phase 1b — Backend Pure Logic Unit Tests ✅ DONE

| ID | Task | Deps | Status |
|---|---|---|---|
| TS-016 | `TokenGeneratorTest`: 43-char URL-safe string; uniqueness across 100 calls; no padding chars | TS-001 | ✅ |
| TS-017 | Email template tests: `InviteEmailTest`, `CoachLinkEmailTest`, `TeamApprovalEmailTest`, `ManagerInviteEmailTest` — HTML string output for given inputs; no I/O | TS-001 | ✅ |
| TS-018 | `JwtTokenTest`: create JWT → extract claims; issuer/audience/expiry fields correct | TS-001 | ✅ |

## Phase 1c — Shared KMP Tests ✅ DONE

| ID | Task | Deps | Status |
|---|---|---|---|
| TS-019 | Add to `shared/build.gradle.kts`: Kotest deps; `commonTest` (kotlin-test, kotest-engine, kotest-assertions); `jvmTest` (sqldelight sqlite-driver, junit-jupiter) | — | ✅ |
| TS-020 | `TeamDomainTest` + `MemberDomainTest`: valid construction, boundary values, role validation | TS-019 | ✅ |
| TS-021 | `EventDomainTest`: event model, date validation | TS-019 | ✅ |
| TS-022 | `AttendanceDomainTest` + `AbwesenheitDomainTest`: state transitions, business rules | TS-019 | ✅ |
| TS-023 | `SerializationRoundTripTest`: all domain models serialize → deserialize → equals | TS-019 | ✅ |
| TS-024 | `RepositoryFakeTest`: in-memory fakes satisfy repository interface contract | TS-019 | ✅ |
| TS-025 | `AttendanceQueriesTest` (jvmTest): in-memory SQLite; insert, selectByEventId, selectByUserId, update status | TS-019 | ✅ |
| TS-026 | `NotificationQueriesTest`: insert, selectUnread, markRead | TS-025 | ✅ |

**Result:** 72 tests, all pass. iOS simulator target skipped (requires Xcode).

## Phase 1d — Admin Panel Tests ✅ DONE

| ID | Task | Deps | Status |
|---|---|---|---|
| TS-027 | Install Vitest 2 + Testing Library + Playwright in `admin/`; configure `vite.config.ts`; add `src/test/setup.ts`; npm scripts `test`, `test:e2e` | — | ✅ |
| TS-028 | `roleLabel.test.ts` + `exportHelpers.test.ts` (CSV string builders) | TS-027 | ✅ |
| TS-029 | `apiClient.test.ts` (mocked fetch) + `authStore.test.ts` | TS-027 | ✅ |
| TS-030 | `loginPage.test.ts` + `clubsPage.test.ts` | TS-027 | ✅ |
| TS-031 | `clubDetailPage.test.ts` + `auditLogPage.test.ts` + `billingPage.test.ts` | TS-027 | ✅ |
| TS-032 | Playwright: `login.spec.ts`, `logout.spec.ts`, `security.spec.ts` (unauthenticated → redirect; XSS escaped) | TS-027 | ✅ |
| TS-033 | Playwright: `clubs.spec.ts`, `export.spec.ts`, `auditLog.spec.ts` | TS-027 | ✅ |

**E2E infrastructure:**
- `docker-compose.yml` at project root: `postgres:16-alpine` on port 5433
- `admin/e2e/globalSetup.ts`: `docker compose up -d --wait db` (ESM `import.meta.url` for `__dirname`)
- `admin/e2e/globalTeardown.ts`: `docker compose down -v` on CI only
- `admin/playwright.config.ts`: Ktor on port **8088** (`PORT=8088` env, avoids local conflicts); SvelteKit gets `PUBLIC_API_URL=http://localhost:8088`
- Ktor `application.conf`: already supports `port = ${?PORT}`
- Added `GET /health` endpoint to Ktor routing (required by Playwright webServer health check)
- `npx playwright install chromium` required once per machine

**Result:** 89 unit tests + 16 E2E tests, all pass.

## Phase 2a — Android UI Tests
**Blocked by:** compose-multiplatform-migration complete

| ID | Task | Deps |
|---|---|---|
| TS-034 | Add Robolectric config to `composeApp/build.gradle.kts`: `isIncludeAndroidResources = true`; add `androidUnitTest` deps (robolectric `4.12.1`, compose-ui-test-junit4 `1.7.1`, ui-test-manifest) | — |
| TS-035 | `LoginScreenTest`: empty submit → inline error; valid submit → success callback; loading state; `ClubDashboardTest`: teams render, empty state, FAB visible to coach | TS-034 |
| TS-036 | `TeamDetailTest`: members list renders, tap navigates; `EventListTest`: events render, calendar tab reachable, form opens | TS-034 |
| TS-037 | `AttendanceListTest`: confirm → CONFIRMED badge; decline → DECLINED badge; `AbsenceSheetTest`: open BegrundungSheet, submit with reason | TS-034 |
| TS-038 | `BottomNavTest`: all tabs reachable, notification badge shows unread count; `OfflineIndicatorTest`: banner visible when NetworkMonitor offline state injected | TS-034 |

## Phase 2b — Shared Composable Tests
**Blocked by:** compose-multiplatform-migration complete

| ID | Task | Deps |
|---|---|---|
| TS-039 | Add `composeApp/commonTest` dep: `ui-test-junit4:1.7.1` (pinned — do not upgrade independently); configure JVM runner for `runComposeUiTest` | — |
| TS-040 | `StatusBadgeTest`: CONFIRMED/DECLINED/PENDING → correct label + color; `EventTypeIndicatorTest`: Training/Match/Other → correct icon; `OfflineIndicatorTest` (shared): shown/hidden based on prop | TS-039 |
| TS-041 | `PlaybookBottomBarTest`: active tab highlighted, badge renders; `AuthStateNavigationTest`: `Unauthenticated` → Login route, `Authenticated` → Dashboard route | TS-039 |

## Phase 2c — iOS Smoke Tests
**Blocked by:** compose-multiplatform-migration complete + Xcode iosApp built

| ID | Task | Deps |
|---|---|---|
| TS-042 | Create `iosAppUITests` Xcode target; `LoginFlowTests`: valid credentials → "Meine Teams" visible; invalid → error shown; `TeamListTests`: team row visible, tap → TeamDetail | — |
| TS-043 | `EventListTests`: event list screen, tap → detail; `AttendanceTests`: confirm → badge updates; `BottomNavTests`: all tabs reachable via bottom bar | TS-042 |

## Phase 3 — Maestro Cross-Layer E2E
**Blocked by:** Phase 2a + 2c complete

| ID | Task | Deps |
|---|---|---|
| TS-044 | Create `maestro/flows/`; `coach-register.yaml` (register → create club → create team → ClubDashboard); `member-invite.yaml` (invite → deep link accept → roster updated) | — |
| TS-045 | `event-attendance.yaml` (create event → member confirms → coach views report); `sa-audit.yaml` (SA login → club view → audit log); `full-journey.yaml` (composite) | TS-044 |

## Phase 4 — CI Pipeline
**Blocked by:** all test phases complete

| ID | Task | Deps |
|---|---|---|
| TS-046 | Add CI jobs: `test:backend` (`./gradlew :backend:test`, Docker runner); `test:shared` (`./gradlew :shared:allTests`, JVM) | — |
| TS-047 | Add CI jobs: `test:android` (`./gradlew :composeApp:testDebugUnitTest`, JVM); `test:admin` (`npm run test && npm run test:e2e`, Node + browser); `test:ios` (`xcodebuild test`, macOS runner); configure parallel Phase 1 jobs | TS-046 |
