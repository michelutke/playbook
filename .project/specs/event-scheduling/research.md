---
template: research
version: 0.1.0
---
# Research: Event Scheduling

## kotlinx-datetime
- **Version**: 0.7.1
- **Docs**: https://github.com/Kotlin/kotlinx-datetime
- **KMP support**: yes — JVM, JS, Native, WasmJs
- **Key findings**:
  - ⚠️ Breaking in 0.7.x: `kotlinx.datetime.Instant` removed; use `kotlin.time.Instant` (stdlib) instead
  - Store `start_at`/`end_at` as `Instant` (UTC) in DB; convert to `LocalDateTime` at display time only
  - `Instant.toLocalDateTime(TimeZone.currentSystemDefault())` for local display
  - Multi-day events: store as `Instant` range; convert each bound to local date for UI
  - DST transitions handled automatically — no manual adjustment needed
  - `DateTimeUnit.DAY` / `DatePeriod` for recurring pattern expansion (e.g. advance by 7 days)
  - Gotcha: `LocalDateTime` has no timezone; never store it — always store `Instant`
- **Decision**: **use** — no alternative for KMP

## Ktor Scheduled Jobs (daily materialisation)
- **Version**: Ktor 3.4.0
- **Docs**: https://flaxoos.github.io/extra-ktor-plugins/ktor-server-task-scheduling/ · https://ktor.io/docs/releases.html
- **KMP support**: N/A (server JVM only)
- **Key findings**:
  - Daily materialisation job expands recurring series 12 months ahead
  - **extra-ktor-plugins task scheduling**: Ktor-native DSL, JDBC lock for distributed safety; cleanest integration
  - **Raw coroutine**: `launch { while(true) { delay(24.hours); expandSeries() } }` — fine for single-instance
  - **Quartz**: persistent, clustered, cron-expression based; heavier but battle-tested
  - Materialisation is idempotent (upsert by `series_id + sequence`); safe to re-run on crash
  - Recommendation: raw coroutines for MVP; extra-ktor-plugins if distributed later
- **Decision**: **raw coroutines for MVP**; revisit if multi-instance

## kizitonwose Calendar (Compose Multiplatform)
- **Version**: 2.9.0 (`com.kizitonwose.calendar:compose-multiplatform`)
- **Docs**: https://github.com/kizitonwose/Calendar
- **KMP support**: yes — Android, iOS, JS, WasmJs, Desktop
- **Key findings**:
  - Purpose-built for Compose Multiplatform; uses `kotlinx-datetime` natively
  - Supports week, month, and year modes with `LazyRow`/`LazyColumn` backing (performant)
  - Fully customisable cell composable — render event dots or time blocks as needed
  - Month view: render dot per event type; tap day → show inline list (matches UX spec S2)
  - Week view: time-block grid requires custom overlay composable; library provides date grid only
  - No built-in time-block (hour grid) view — week time-grid must be implemented manually on top
  - 100+ events: dot-only month cells render fine; week time-blocks need lazy rendering of visible range only
  - Actively maintained; latest release (2.9.0) bumped to Compose 1.9.x and Kotlin 2.2.x
- **Decision**: **use** for month view; **custom implementation** for week time-block grid on top of library primitives
