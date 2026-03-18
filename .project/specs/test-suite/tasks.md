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

## Phase 2a — Android UI Tests ✅ DONE

| ID | Task | Deps | Status |
|---|---|---|---|
| TS-034 | Add Robolectric config to `composeApp/build.gradle.kts`: `isIncludeAndroidResources = true`; add `androidUnitTest` deps (robolectric `4.12.1`, compose-ui-test-junit4 `1.7.1`, ui-test-manifest) | — | ✅ |
| TS-035 | `LoginScreenTest` + `ClubDashboardScreenTest` | TS-034 | ✅ |
| TS-036 | `TeamDetailScreenTest` + `EventListScreenTest` | TS-034 | ✅ |
| TS-037 | `AttendanceListScreenTest` + `MyAbsencesScreenTest` | TS-034 | ✅ |
| TS-038 | `BottomNavTest` + `OfflineIndicatorTest` | TS-034 | ✅ |

**Result:** 30 tests, all pass. Fakes in `composeApp/src/androidUnitTest/kotlin/ch/teamorg/test/FakeRepositories.kt`.

## Phase 2b — Shared Composable Tests ✅ DONE

| ID | Task | Deps | Status |
|---|---|---|---|
| TS-039 | Add `commonTest` dep: `ui-test-junit4:1.7.1` (pinned); add `androidUnitTest` test deps | — | ✅ |
| TS-040 | `StatusBadgeTest` + `EventTypeIndicatorTest` + `OfflineIndicatorTest` | TS-039 | ✅ |
| TS-041 | `TeamorgBottomBarTest` + `AuthStateTest` | TS-039 | ✅ |

## Phase 2c — iOS Smoke Tests ✅ DONE

| ID | Task | Deps | Status |
|---|---|---|---|
| TS-042 | Create `iosAppUITests` Xcode target; `LoginFlowTests` + `TeamListTests` | — | ✅ |
| TS-043 | `EventListTests` + `AttendanceTests` + `BottomNavTests` | TS-042 | ✅ |

**Implementation notes:**
- `iosApp/project.yml`: added `iosAppUITests` ui-testing target; run `xcodegen generate` to apply
- `.testTag()` accessibilityIdentifiers added to: `LoginScreen`, `ClubDashboardScreen`, `TeamDetailScreen`, `EventListScreen`, `AttendanceListScreen`, `TeamorgBottomBar`
- `TeamorgUITestCase` base class: shared `login()` helper + `XCTSkip` guard when backend unavailable
- All post-login tests skip gracefully when backend is not running (CI without server)

## Phase 3 — Maestro Cross-Layer E2E ✅ DONE

| ID | Task | Deps | Status |
|---|---|---|---|
| TS-044 | `maestro/flows/coach-register.yaml` + `member-invite.yaml` | — | ✅ |
| TS-045 | `event-attendance.yaml` + `sa-audit.yaml` + `full-journey.yaml` | TS-044 | ✅ |

## Phase 4 — CI Pipeline ✅ DONE

| ID | Task | Deps | Status |
|---|---|---|---|
| TS-046 | GitHub Actions: `test-backend` + `test-shared` jobs | — | ✅ |
| TS-047 | GitHub Actions: `test-android` + `test-admin` + `test-ios` jobs; Phase 1 parallel config | TS-046 | ✅ |

**CI file:** `.github/workflows/test.yml` — Phase 1 jobs run in parallel on PR; Phase 2 jobs require Phase 1 to pass.
