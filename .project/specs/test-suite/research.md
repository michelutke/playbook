---
template: research
version: 0.1.0
gate: READY SET
---
# Research: Test Suite

## Verdict: Feasible — 5 issues need resolution before READY GO

The spec is largely implementable. Tooling is available for every layer. Five issues require resolution — one is a critical scope mismatch, the rest are configuration/API constraints.

---

## Validated Items

### Backend — Ktor testApplication: CONFIRMED, CORRECT CHOICE

`io.ktor:ktor-server-tests:3.1.0` provides `testApplication {}` — full in-process application context, no real HTTP. Supports auth plugins, routing, JWT, Koin injection. Works exactly as the spec intends.

**Key caveat:** the old `withTestApplication` / `handleRequest` API is **deprecated** — only `testApplication {}` is valid for Ktor 3.

### Backend — Testcontainers + PostgreSQL: CONFIRMED

`org.testcontainers:postgresql:1.20.2` + `junit-jupiter:1.20.2` works with JUnit 5 on JVM. Container start time ~3–5s per test class (shared with `@Container` companion object). Flyway migrations apply during test setup — validates V1→latest chain as required.

**CI note:** requires Docker daemon. Must be configured in CI (Docker-in-Docker or DinD socket mount). Not a blocker but a setup task.

### Shared / commonTest — coroutines-test: CONFIRMED

`kotlinx-coroutines-test:1.9.0` (aligns with coroutines version in project) supports `runTest {}` with virtual time in `commonTest` across JVM/Android/iOS native. No platform-specific gotchas.

### Admin (SvelteKit) — Vitest + Playwright: LOW RISK

Both are stable and independent of KMP/CMP. No compatibility concerns. Standard setup.

### Kotest — CONFIRMED COMPATIBLE

Kotest 5.10.0 supports Kotlin 2.1.10 + KMP. Usable in `commonTest` for assertions and test spec styles. Requires KSP plugin. JVM side is fully featured; iOS native runner has minor limitations (no annotation reflection for some discovery patterns — use `FunSpec` or `StringSpec` which work via compiler plugin).

---

## Issues Found: 5 Items

### ISSUE 1: Backend Has No Service Layer — Scope Mismatch (CRITICAL)

**Req.md states:** "Unit tests for service/use-case layer: isolated, no DB, all dependencies mocked"

**Reality:** The backend has **no service/use-case layer**. Routes call DB repositories directly:
```
routes/ → db/repositories/ → Exposed + DB
```
There is nothing to unit-test in isolation. The "service layer" doesn't exist.

**Options (choose one before READY GO):**

A) **Redefine "unit tests" → route-level tests via `testApplication`** (mocked DB layer). This collapses the unit/integration distinction for backend — all tests become integration tests at different scopes. Simple but no isolated logic tests.

B) **Extract a thin service layer** before writing tests. Routes delegate to service classes; services hold business logic (invite expiry, JWT validation, audit formatting, etc.). Services are then testable in isolation with mocked repos. This matches the req.md intent but requires refactoring first.

C) **Skip unit tests, integration-only backend**. Testcontainers integration tests cover all routes end-to-end. Sufficient for MVP but misses isolated logic test coverage.

**Recommendation:** Option A or C. Option B is YAGNI scope creep unless there's genuine complex business logic that warrants it.

---

### ISSUE 2: SQLDelight JDBC Driver — commonTest Platform Constraint

**Req.md states:** "SQLDelight query tests (in-memory SQLite driver)" in `shared/commonTest`

**Reality:** The `jdbc-driver` (in-memory SQLite) **only compiles and runs on JVM**. If `commonTest` runs on iOS native target (`iosSimulatorArm64Test`), JDBC imports will fail to compile.

**Fix:** Move SQLDelight tests to `jvmTest` source set (not `commonTest`):
```kotlin
// shared/build.gradle.kts
jvmTest {
    dependencies {
        implementation("app.cash.sqldelight:sqlite-driver:2.0.2")
    }
}
```

Alternatively, use `expect/actual` driver per platform — more work, broader coverage. `jvmTest` is the simpler path for this project.

---

### ISSUE 3: Robolectric in composeApp KMP Module — Non-trivial Gradle Setup

**Post-CMP migration**, Android UI tests live in `composeApp/src/androidTest`. Robolectric works in the Android source set of a KMP module but requires explicit Gradle config:

```kotlin
// composeApp/build.gradle.kts
android {
    testOptions {
        unitTests {
            isIncludeAndroidResources = true  // required for Robolectric
        }
    }
}
```

Plus each test class needs `@RunWith(AndroidJUnit4::class)` + `@Config(sdk = [34])`.

**Additional constraint:** Robolectric runs on JVM only — no iOS equivalent. Tests that are logically "Android smoke flows" cannot share logic with iOS. The spec correctly separates these.

**Dependency note:** This entire section is blocked until `composeApp/` module exists (i.e., CMP migration complete). Tech spec should note the Gradle config requirement.

---

### ISSUE 4: CMP iOS UI Testing — Experimental API, Limited Automation

**Req.md states:** iOS smoke tests (login → home, team list, attendance, etc.) "Tooling TBD in tech spec"

**Reality (as of CMP 1.7.x):**

| Approach | Status | Usable? |
|----------|--------|---------|
| `runComposeUiTest {}` in `commonTest` | **Experimental** — JetBrains API | ⚠️ Yes, but unstable API |
| `iosSimulatorArm64Test` Kotlin unit tests | Stable | ✅ For logic only |
| XCTest / Swift Testing in Xcode | Stable, mature | ✅ For UI smoke flows |
| Maestro (cross-platform E2E) | Stable | ✅ For full journey tests |

**Recommendation for tech spec:**
- iOS "smoke flows" in req.md → implement as **XCTest UI tests** in `iosApp/` Xcode project (Swift), not Kotlin
- `commonTest` `runComposeUiTest {}` for shared composable unit logic (experimental — pin CMP version, don't upgrade mid-sprint)
- Maestro as the cross-layer E2E runner (item 6 of the spec) — covers both platforms from one YAML-based script

This is consistent with the req.md's "Tooling TBD" acknowledgement. Tech spec must make this choice explicit.

---

### ISSUE 5: Dependency Ordering — Some Tests Can Start Now

**Req.md states:** "Blocked by compose-multiplatform-migration"

**True but overly broad.** Three test areas are fully independent of CMP migration:

| Area | CMP dependency? | Can start now? |
|------|----------------|---------------|
| `:backend` unit + integration tests | None | ✅ Yes |
| `:shared` commonTest / jvmTest | None | ✅ Yes |
| `admin/` Vitest + Playwright | None | ✅ Yes |
| `:composeApp` Android UI tests | ❌ Needs composeApp module | No |
| iOS tests | ❌ Needs CMP migration + iosApp | No |

**Recommendation:** Start backend + shared + admin tests in parallel with CMP migration to reduce total timeline.

---

## Library Version Verification

Verify before writing tech spec:

| Library | Expected Version | Check |
|---------|-----------------|-------|
| `ktor-server-tests` | `3.1.0` | Match ktor version |
| `testcontainers:postgresql` | `1.20.2` | Maven Central |
| `kotlinx-coroutines-test` | `1.9.0` | Match coroutines version |
| `robolectric` | `4.12.1` | Check AGP 8.5 compatibility note |
| `compose-ui-test-junit4` | Match CMP `1.7.1` | Same BOM as composeApp |
| `kotest-framework-engine` | `5.10.0` | Kotlin 2.1.10 compat |
| `junit-jupiter` | `5.10.x` | JUnit 5 for backend |

---

## Pitfall Matrix

| Pitfall | Present? | Status |
|---------|---------|--------|
| Backend has no service layer to unit test | Yes | See ISSUE 1 — must decide approach |
| SQLDelight JDBC driver fails on iOS native target | Yes | See ISSUE 2 — use jvmTest |
| Robolectric needs special Gradle config in KMP module | Yes | See ISSUE 3 — documented |
| CMP iOS UI testing API is experimental | Yes | See ISSUE 4 — use XCTest + Maestro |
| Testcontainers requires Docker in CI | Yes | CI config task — known |
| `withTestApplication` deprecated in Ktor 3 | N/A | Use `testApplication {}` only |
| `createAndroidComposeRule` is Android-only | N/A | Use `runComposeUiTest` in commonTest |
| `collectAsStateWithLifecycle` not in commonTest | N/A | Already handled in CMP migration |

---

## Summary

**Spec is implementable.** Required decisions before READY GO:

1. **Choose backend unit test strategy** (ISSUE 1 — scope decision, not a tech problem)
2. **Move SQLDelight tests to `jvmTest`**, not `commonTest` (ISSUE 2 — Gradle change)
3. **Document Robolectric Gradle config** for `composeApp/` KMP module (ISSUE 3 — tech spec gap)
4. **Choose iOS test tooling**: XCTest for smoke flows, Maestro for E2E, experimental `runComposeUiTest` for shared composable logic (ISSUE 4 — tech spec decision)
5. **Split blocked/unblocked work** in implementation phases — backend+shared+admin tests can run in parallel with CMP migration (ISSUE 5 — planning)
