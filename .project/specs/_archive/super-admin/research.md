---
template: research
version: 0.1.0
---
# Research: Super Admin

## Ktor 3.x + JWT Auth
- **Version**: 3.4.0
- **Docs**: https://ktor.io/docs/server-jwt.html · https://blog.jetbrains.com/kotlin/2026/01/ktor-3-4-0-is-now-available/
- **KMP support**: N/A (server JVM)
- **Key findings**:
  - Plugin: `io.ktor:ktor-server-auth-jwt` — configure via `install(Authentication) { jwt("sa") { ... } }`
  - Custom claims readable via `JWTPrincipal.payload.getClaim("impersonated_by", String::class)`
  - `requireSuperAdmin()` middleware: validate JWT, then query `users.super_admin` from DB on every request (not trusted from JWT)
  - Impersonation JWT: same library, separate `jwt("impersonation")` config with stricter expiry (3600s)
  - Separate `jwt-creator` utility to mint short-lived impersonation tokens with custom claims
  - Ktor 3.4 includes OpenAPI generation and improved structured concurrency for request lifecycle
  - CORS: `install(CORS)` with explicit allowed origin for Svelte admin panel
  - Content negotiation: `install(ContentNegotiation) { json() }` with `kotlinx.serialization`
- **Decision**: **use** — first-party plugin, well-documented

## SvelteKit 2 + Svelte 5
- **Version**: SvelteKit 2.x / Svelte 5
- **Docs**: https://kit.svelte.dev · https://svelte.dev
- **KMP support**: N/A (web frontend)
- **Key findings**:
  - SvelteKit 2 is the current stable major; supports Vite 7 + Rolldown in latest releases
  - Svelte 5 introduces runes (`$state`, `$derived`, `$effect`) — new reactivity model; preferred for new projects
  - SvelteKit recommended over plain Svelte for admin panel: built-in routing, server-side load functions, form actions
  - Route guards: `+layout.server.ts` load function checks session cookie; redirects to login if invalid
  - Auth: store SA JWT in httpOnly cookie; SvelteKit load functions attach to API requests
  - Tailwind CSS 4 integrates cleanly with SvelteKit via Vite plugin
  - For admin panel: SPA mode (`export const prerender = false; export const ssr = false`) simplest for pure client-side panel behind auth
  - Flowbite-Svelte or shadcn-svelte for component library (data tables, modals, forms)
- **Decision**: **use SvelteKit 2 + Svelte 5** — better DX than plain Svelte; routing and server functions useful for SA panel

## Async Job Pattern (CSV export)
- **Version**: Ktor 3.4.0 (coroutines built-in)
- **Docs**: https://ktor.io/docs/releases.html
- **KMP support**: N/A (server JVM)
- **Key findings**:
  - Pattern: `POST /sa/audit-log/export` → launch coroutine job → return `{ job_id }`; `GET .../export/{jobId}` polls
  - Job state stored in `export_jobs` table (`pending | processing | done | failed`) with result file path
  - On `done`: serve file from local storage as streaming response, or return pre-signed path for client to download directly
  - Simple approach: `CoroutineScope(Dispatchers.IO).launch { generateCsv(); markDone() }` — no external dep
  - File: write CSV to temp directory, serve via `respondFile()` or static route
  - Cleanup: scheduled job deletes export files older than 1 hour
  - No need for external job queue (Kafka, Redis queues) at this scale
- **Decision**: **coroutine + DB state table** — sufficient for low-volume SA export; no external queue needed

## Koin DI (KMP + Ktor)
- **Version**: 4.1.1
- **Docs**: https://insert-koin.io/docs/reference/koin-mp/kmp/ · https://insert-koin.io/docs/setup/koin/
- **KMP support**: yes — full KMP support; `koin-ktor` merged (no longer `koin-ktor3`)
- **Key findings**:
  - Koin 4.x built on Kotlin 2.0; `koin-ktor` now compiled as KMP artifact (target from multiplatform module)
  - Ktor 3.4 DI Bridge: bidirectional dependency resolution between Koin and Ktor's own DI — opt-in
  - `startKoin { modules(...) }` in `Application.module()`; use `get<T>()` in route handlers
  - Shared KMP modules: define in `commonMain`; platform-specific bindings in `androidMain`/`iosMain`/`jvmMain`
  - Annotation-free; no codegen required
  - SA panel is Svelte (no Koin); Koin scoped to Ktor server + KMP shared code only
- **Decision**: **use** — standard for this stack
