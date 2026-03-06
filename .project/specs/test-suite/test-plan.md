---
template: test-plan
version: 0.1.0
---
# Test Plan: Test Suite

## Verification Strategy

Each phase has a binary exit gate. All Phase 1 gates can be verified in parallel. Phase 2+ gates require compose-multiplatform-migration complete.

---

## Phase Exit Gates

| Phase | Command | Pass Criteria |
|-------|---------|--------------|
| 1a — Backend integration | `./gradlew :backend:test` | All route + security tests green; no 500s on malformed input |
| 1b — Backend unit | `./gradlew :backend:test` | TokenGenerator, email, JWT tests green with no infrastructure |
| 1c — Shared KMP | `./gradlew :shared:allTests` | commonTest (Kotest) + jvmTest (JUnit 5 + SQLite) all green |
| 1d — Admin | `npm run test && npm run test:e2e` | Vitest unit + component + Playwright E2E all green |
| 2a — Android UI | `./gradlew :composeApp:testDebugUnitTest` | All Robolectric smoke flows green |
| 2b — Shared composables | `./gradlew :composeApp:commonTest` | All `runComposeUiTest` assertions pass on JVM runner |
| 2c — iOS smoke | `xcodebuild test -scheme iosApp` | All XCTest flows pass on iOS 16 simulator |
| 3 — Maestro E2E | `maestro test maestro/flows/full-journey.yaml` | Full journey passes on Android emulator + iOS simulator |
| 4 — CI | PR pipeline | All jobs green on every PR; zero flaky tests |

---

## Coverage Targets

| Layer | Target | Rationale |
|-------|--------|-----------|
| Backend routes | All route groups covered | Integration-first; no service layer to mock |
| Backend security | All OWASP items documented + tested | High risk; explicitly required |
| Shared domain | All domain models have at least happy path + invalid state | Business logic must be reliable |
| SQLDelight queries | insert / select / update / delete for each query file | Query correctness; cheap to test |
| Admin unit | All utility functions | Pure functions; trivial to test |
| Admin components | All interactive components | Login, club list, audit log are critical paths |
| Android UI | All primary screens have a smoke test | Regression guard post-CMP migration |
| iOS smoke | Login + nav + core flows | Minimal viable iOS test coverage |

---

## Security Test Cases (Phase 1a)

Documented per endpoint in `SecurityTest.kt` — reproduced here for review.

| # | Probe | Expected Response |
|---|-------|------------------|
| SEC-01 | Any protected endpoint without Authorization header | 401 |
| SEC-02 | Player JWT on `POST /events` (coach-only) | 403 |
| SEC-03 | Player JWT on `DELETE /teams/:id` | 403 |
| SEC-04 | Regular JWT on `GET /sa/clubs` | 403 |
| SEC-05 | Expired JWT (past `exp` claim) | 401 |
| SEC-06 | Tampered JWT (invalid HMAC signature) | 401 |
| SEC-07 | Malformed JSON body on any POST | 400 (not 500) |
| SEC-08 | `';DROP TABLE teams;--` in team name search param | 200 with empty results or 400; no 500 |
| SEC-09 | `<script>alert(1)</script>` in club name | Stored as literal; returned escaped in JSON |
| SEC-10 | Oversized request body (>1MB) | 400 or 413 |

---

## Test Data Isolation

All integration tests must be independent:

- `IntegrationTestBase.resetSchema()` called `@BeforeAll` — clean DB per test class
- Each test class seeds its own fixtures in `@BeforeEach` via `Fixtures.seedCoachAndTeam(db)`
- `resetSchema()` has localhost guard — refuses to run against non-test DB
- Testcontainers PostgreSQL container shared across all test classes (starts once per JVM)

---

## Flakiness Prevention

| Risk | Mitigation |
|------|-----------|
| Testcontainers cold start | `testcontainers.reuse.enable=true` in `~/.testcontainers.properties` (local dev only) |
| Robolectric not rendering Compose | Verify `isIncludeAndroidResources = true` is set; fail fast with explicit error |
| `runComposeUiTest` API drift | Pin CMP `1.7.1` in version catalog; flag any CMP upgrade in composeApp |
| XCTest `testTag` missing | Track `testTag` coverage during CMP migration; blocker for Phase 2c |
| Playwright flakiness | `waitFor` assertions on visible elements; no fixed `sleep()` calls |
| Kotest KSP mismatch | Pin `ksp = "2.1.10-1.0.29"`; update only when Kotlin version updates |
