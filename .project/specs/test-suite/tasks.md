---
template: tasks
version: 0.1.0
gate: READY GO
---
# Tasks: Test Suite

## Phase 1a — Backend Integration + Security Tests

| ID | Task | Deps |
|---|---|---|
| TS-001 | Add test deps to `backend/build.gradle.kts`: Ktor server tests, JUnit 5, Testcontainers (core + junit-jupiter + postgresql), Koin test; `useJUnitPlatform()` | — |
| TS-002 | Create `IntegrationTestBase.kt`: `@Testcontainers` PostgreSQL container, `testApp {}` helper with `MapApplicationConfig`, `bearerToken()` / `saToken()` helpers, `resetSchema()` with localhost guard | TS-001 |
| TS-003 | Create `Fixtures.kt`: test UUIDs (`TEST_COACH_ID`, `TEST_PLAYER_ID`, `TEST_CLUB_ID`, `TEST_TEAM_ID`); `seedCoachAndTeam(db)` helper | TS-002 |
| TS-004 | `AuthRoutesTest`: happy path + 400/401 for POST /register, /login, /refresh, /logout | TS-003 |
| TS-005 | `ClubRoutesTest`: GET/POST/PUT /clubs, /clubs/:id — happy path + 401 + 403 + 404 | TS-003 |
| TS-006 | `TeamRoutesTest`: GET/POST/PUT/DELETE /teams, /teams/:id — happy path + 401 + 403 + 404 | TS-003 |
| TS-007 | `MemberRoutesTest`: GET /teams/:id/members, DELETE membership | TS-003 |
| TS-008 | `InviteRoutesTest`: POST /invites, GET /invites/:token (200 + 410), POST /invites/:token/accept | TS-003 |
| TS-009 | `CoachLinkRoutesTest`: POST /coach-links, POST /coach-links/:token/accept | TS-003 |
| TS-010 | `EventRoutesTest`: GET/POST/PUT/DELETE /events; subgroup assignment | TS-003 |
| TS-011 | `AttendanceRoutesTest`: POST confirm/decline, GET by event, check-in toggle | TS-003 |
| TS-012 | `AbwesenheitRoutesTest`: POST /abwesenheit, GET by user, GET by team | TS-003 |
| TS-013 | `NotificationRoutesTest`: GET /notifications (unread), POST preferences | TS-003 |
| TS-014 | `SuperAdminRoutesTest`: GET /sa/clubs, /sa/teams, export endpoint, audit log | TS-003 |
| TS-015 | `SecurityTest`: unauthenticated 401 all protected routes; player 403 on coach-only; regular JWT 403 on SA routes; expired JWT 401; tampered JWT 401; malformed JSON 400; oversized payload 400/413; SQL injection probes on query params; OWASP comments per endpoint | TS-002 |

## Phase 1b — Backend Pure Logic Unit Tests

| ID | Task | Deps |
|---|---|---|
| TS-016 | `TokenGeneratorTest`: 43-char URL-safe string; uniqueness across 100 calls; no padding chars | TS-001 |
| TS-017 | Email template tests: `InviteEmailTest`, `CoachLinkEmailTest`, `TeamApprovalEmailTest`, `ManagerInviteEmailTest` — HTML string output for given inputs; no I/O | TS-001 |
| TS-018 | `JwtTokenTest`: create JWT → extract claims; issuer/audience/expiry fields correct | TS-001 |

## Phase 1c — Shared KMP Tests

| ID | Task | Deps |
|---|---|---|
| TS-019 | Add to `shared/build.gradle.kts`: KSP plugin `2.1.10-1.0.29` + Kotest multiplatform plugin `5.10.0`; `commonTest` deps (kotlin-test, coroutines-test, kotest-engine, kotest-assertions); `jvmTest` deps (sqldelight sqlite-driver, junit-jupiter); KSP `kspCommonMainMetadata` for Kotest | — |
| TS-020 | `TeamDomainTest` + `MemberDomainTest` (Kotest `StringSpec`): valid construction, boundary values, invalid state rejection, role validation, invite status transitions | TS-019 |
| TS-021 | `EventDomainTest`: event model, date validation, subgroup assignment rules | TS-019 |
| TS-022 | `AttendanceDomainTest`: PENDING → CONFIRMED / DECLINED transitions, business rules; `AbwesenheitDomainTest`: creation, period overlap detection | TS-019 |
| TS-023 | `SerializationRoundTripTest`: all domain models serialize → deserialize → equals (Kotest) | TS-019 |
| TS-024 | `RepositoryFakeTest`: in-memory fake repos satisfy repository interface contract | TS-019 |
| TS-025 | `AttendanceQueriesTest` (jvmTest): `@BeforeEach` in-memory driver + `Schema.create`; insert, selectByEventId, selectByUserId, update status | TS-019 |
| TS-026 | `EventQueriesTest`: insert, selectByTeamId, selectById, delete; `NotificationQueriesTest`: insert, selectUnread, markRead | TS-025 |

## Phase 1d — Admin Panel Tests

| ID | Task | Deps |
|---|---|---|
| TS-027 | Install dev deps in `admin/`: vitest `^2.1.0`, `@vitest/ui`, `@testing-library/svelte ^5.2.0`, `@testing-library/jest-dom ^6.4.0`, `@playwright/test ^1.41.0`, `jsdom ^25.0.0`; configure `vite.config.ts` test block (jsdom env, globals, setupFiles); add `src/test/setup.ts`; add npm scripts `test`, `test:ui`, `test:e2e` | — |
| TS-028 | `dateUtils.test.ts` (date formatting, relative time, locale); `roleLabel.test.ts` (coach/player/SA label mapping); `exportHelpers.test.ts` (CSV string builders) | TS-027 |
| TS-029 | `apiClient.test.ts` (mocked fetch: auth header injection, 401 handling, error propagation); `authStore.test.ts` (login sets token, logout clears token) | TS-027 |
| TS-030 | `LoginForm.test.ts`: renders fields; valid submit success; 401 shows error; `ClubList.test.ts`: rows render, search filter, pagination | TS-027 |
| TS-031 | `TeamDetail.test.ts`: member list renders, role chips correct; `AuditLog.test.ts`: entries with timestamps + actor names; `ExportButton.test.ts`: click triggers download, loading state shown | TS-027 |
| TS-032 | Playwright: `login.spec.ts` (SA login → dashboard), `logout.spec.ts` (logout → login redirect), `security.spec.ts` (unauthenticated → redirect; XSS: rendered content escaped) | TS-027 |
| TS-033 | Playwright: `clubs.spec.ts` (search → detail → teams), `export.spec.ts` (CSV downloaded), `auditLog.spec.ts` (entries visible) | TS-027 |

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
