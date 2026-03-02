---
template: research
version: 0.1.0
---
# Research: Attendance Tracking

## SQLDelight
- **Version**: 2.2.1
- **Docs**: https://sqldelight.github.io/sqldelight/2.2.1/
- **KMP support**: yes — Android, iOS/Native, JVM, JS
- **Key findings**:
  - Group ID changed from `com.squareup.sqldelight` → `app.cash.sqldelight` in 2.0
  - Define schema in `.sq` files; generates typesafe Kotlin APIs at compile time
  - Flow/coroutines integration via `asFlow()` and `mapToList()` extension on queries
  - Drivers: `AndroidSqliteDriver` (Android), `NativeSqliteDriver` (iOS), `JdbcSqliteDriver` (JVM tests)
  - `commonMain` holds `.sq` files; platform drivers injected via expect/actual or DI
  - Migrations handled via `.sqm` files with version numbers
  - Supports insert-or-replace (upsert) for offline mutation queue pattern
  - No built-in encryption; use SQLCipher driver if data-at-rest encryption needed
- **Decision**: **use** — the standard for KMP local persistence

## kotlinx-datetime
- **Version**: 0.7.1
- **Docs**: https://github.com/Kotlin/kotlinx-datetime
- **KMP support**: yes — JVM, JS, Native, WasmJs
- **Key findings**:
  - ⚠️ Breaking: 0.7.x removes `kotlinx.datetime.Instant` in favour of `kotlin.time.Instant` (stdlib); update imports
  - `Instant` (UTC) is the correct type for storing `responded_at`, `updated_at`
  - `LocalDateTime` is timezone-unaware; always pair with explicit `TimeZone` for display
  - `TimeZone.currentSystemDefault()` for user's local timezone; `TimeZone.of("Europe/Zurich")` for explicit
  - DST handled automatically via bundled IANA timezone database
  - Serialization: `kotlinx.serialization` support available via `kotlinx-datetime` serializers
  - Deadline comparison: `Clock.System.now() > deadline.toInstant(tz)` in shared domain
- **Decision**: **use** — official KMP datetime library; no alternative

## Ktor Background Jobs (server-side)
- **Version**: Ktor 3.4.0; extra-ktor-plugins task scheduling (community)
- **Docs**: https://flaxoos.github.io/extra-ktor-plugins/ktor-server-task-scheduling/
- **KMP support**: N/A (server JVM only)
- **Key findings**:
  - **Option A — extra-ktor-plugins task scheduling**: Ktor plugin DSL, supports Redis/JDBC/MongoDB lock for distributed coordination; most ergonomic for Ktor projects
  - **Option B — raw coroutines**: `launch { while(true) { delay(interval); job() } }` in `Application.module()`; zero deps; suitable for single-instance MVP
  - **Option C — Quartz**: mature, persistent, distributed; overkill for daily/5-min jobs at this scale
  - Abwesenheit backfill: one-off triggered job (not periodic); raw coroutine launch or simple job queue suffices
  - Post-event auto-present: event-time triggered; can use a periodic checker or schedule via coroutine delay
  - For MVP single-instance: raw coroutine approach is sufficient; migrate to extra-ktor-plugins if multi-instance needed
- **Decision**: **raw coroutines for MVP**; **consider extra-ktor-plugins** if multi-instance deployment
