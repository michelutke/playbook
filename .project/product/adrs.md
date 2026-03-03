---
template: adrs
version: 0.1.0
---
# Architecture Decision Records: Playbook

## ADR-001: Web Frontend — Compose Multiplatform Web vs Svelte

**Date:** 2026-02-27
**Status:** Partially resolved
**Context:** The app targets mobile (iOS/Android) + web for all users. CMP Web (Wasm/JS) would allow a single Kotlin/Compose codebase across all platforms. However, CMP web maturity, performance, bundle size, and accessibility may make Svelte + Ktor a better fit.
**Decision:**
- **SuperAdmin panel**: SvelteKit — decided independently; SA is always web-only, no value in CMP here
- **Main app web**: deferred — pending further research on CMP Web maturity
**Consequences:**
- SvelteKit admin at `admin/` already built and shipped
- Main app web client still TBD; stats aggregation is client-side (ADR-007), so a web client would need to replicate aggregation or a server stats endpoint would need to be added

---

## ADR-002: Monorepo

**Date:** 2026-02-27
**Status:** Decided — Monorepo
**Context:** KMP project naturally groups mobile + shared code. Backend and admin could live separately.
**Decision:** Single monorepo containing all components.
**Structure:**
```
/
├── shared/       — KMP shared domain + data
├── androidApp/   — Android Compose entry point
├── iosApp/       — iOS (CMP; minimal native code)
├── backend/      — Ktor JVM server
└── admin/        — SvelteKit SuperAdmin panel
```
**Consequences:** Unified dependency management; atomic commits across layers; Gradle + npm in same repo requires discipline to keep build scripts separate.

---

## ADR-003: Database — PostgreSQL + Exposed ORM + HikariCP + Flyway

**Date:** 2026-03-01
**Status:** Decided
**Context:** Relational data model with UUIDs, ENUMs, JSONB, and TIMESTAMPTZ. Need a JVM-compatible ORM.
**Decision:** PostgreSQL with Jetbrains Exposed 0.54.0 (DSL mode), HikariCP 5.1.0 connection pool, Flyway 10.18.0 migrations, PostgreSQL JDBC 42.7.3.
**Consequences:**
- Exposed DSL gives type-safe queries without full ORM complexity
- Flyway versioned migrations in `backend/src/main/resources/db/migrations/` (V1–V21+)
- ENUM types defined in Kotlin and mapped to PG ENUMs in migrations

---

## ADR-004: Push Notifications — OneSignal behind PushService abstraction

**Date:** 2026-03-01
**Status:** Decided
**Context:** Need cross-platform push (iOS APNs + Android FCM) without managing provider SDKs directly on backend.
**Decision:** OneSignal as active provider, abstracted behind `PushService` interface. Our `user_id` = OneSignal external user ID.
**Consequences:**
- Provider can be swapped by binding a different `PushService` — trigger logic unchanged
- Push tokens stored in `push_tokens` table; upserted per app launch; deregistered on logout
- iOS: 2 manual Xcode steps required (NT-011, NT-016) — see `iosApp/README.md`

---

## ADR-005: Local Cache — SQLDelight (KMP)

**Date:** 2026-03-01
**Status:** Decided
**Context:** Offline-first NFR for attendance responses and notification inbox.
**Decision:** SQLDelight 2.0.2 for KMP local cache. Platform drivers injected via Koin.
**Consequences:**
- SQLDelight `.sq` files in `shared/src/commonMain/db/`
- Offline mutation queue for attendance; server-authoritative on sync conflicts (deadline = 409)
- Unread notification count derived from local cache — no extra API call

---

## ADR-006: Background Jobs — In-process Ktor coroutines (MVP)

**Date:** 2026-03-01
**Status:** Decided (revisit at scale)
**Context:** Several features need background jobs: event materialisation, abwesenheit backfill, reminder scheduler, auto-present, audit export, audit retention.
**Decision:** All jobs run as Ktor coroutines in the server process. No external job queue for MVP.
**Consequences:**
- No Redis/worker infrastructure needed
- Job loss on process crash; restart on next server start — acceptable for MVP
- At scale, notification fan-out for large teams may need an external queue

---

## ADR-007: Stats Aggregation — Client-side

**Date:** 2026-03-01
**Status:** Decided
**Context:** Attendance stats (presence_pct, training_pct, match_pct) could be computed server-side or client-side.
**Decision:** No server stats endpoint. Backend returns raw rows; aggregation runs in shared KMP domain on-device.
**Consequences:**
- Simpler backend; filters applied locally without extra API calls
- A future web client must replicate aggregation logic or a server endpoint must be added

---

## ADR-008: Role Checking — DB per request (not JWT claims)

**Date:** 2026-03-01
**Status:** Decided
**Context:** Role changes must take effect immediately without requiring re-login.
**Decision:** Roles in `team_memberships`, checked from DB per request. JWT contains only `user_id`.
**Consequences:**
- Immediate role effect — no token invalidation needed
- Extra DB query per role-gated request (acceptable at MVP scale)
- SuperAdmin flag (`users.super_admin`) is the one exception; also DB-checked
