---
template: anti-patterns
version: 0.1.0
---
# Anti-Patterns: Playbook

## Architecture Anti-Patterns
- Putting business logic in UI layer (Composables / Views) — keep it in shared KMP modules
- Skipping the shared KMP layer and duplicating logic per platform
- Tight coupling between backend API shape and UI models (use domain models as buffer)
- **Excessive platform-specific iOS code** — custom Swift/UIKit interop is a last resort; keep UI and logic in shared CMP. Every `expect/actual` must be justified.

## Code Anti-Patterns
- Force-unwrapping / unchecked casts without justification
- Hardcoded strings for user-facing text (use resource system for i18n from day 1)
- Mixing coroutine scopes carelessly — define lifecycle-aware scopes explicitly
- Running background jobs synchronously on the request path — always dispatch to a background coroutine

## Process Anti-Patterns
- Building web UI before ADR-001 (CMP Web vs Svelte) is resolved (for main app)
- Designing backend API without confirming client requirements first
- Skipping offline-first design decisions late in the cycle

## Lessons Learned

### Background Jobs
Don't run async work (backfill, fan-out, export) on the request path. Every job that could take >100ms must be dispatched to a background coroutine. Blocking the request on job completion is always wrong.

### Stats: No Server-side Endpoint
Don't add server-side aggregation endpoints prematurely. Raw rows + client-side aggregation (in shared KMP) is simpler and enables flexible filtering without extra API calls. Only add a server stats endpoint if a thin client (web) genuinely can't aggregate.

### Push Provider Abstraction
Never call a push provider SDK directly from trigger logic. Always go through a `PushService` interface. OneSignal, FCM direct, APNs direct — they're all replaceable. The abstraction pays off immediately if the provider has downtime or you need to switch.

### Roles in DB, Not JWT
Don't embed roles in JWT claims. Role changes must be immediate. Store roles in DB, check per request. The extra query is negligible at MVP scale. Embedding roles in JWT requires token invalidation infrastructure that isn't worth building early.

### Invite Token Security
Use `SecureRandom` + URL-safe Base64 for invite tokens (not UUIDs). UUID v4 leaks version/variant bits. 32 bytes = ~256-bit entropy. Store raw token with unique index. Don't store hashed tokens unless you're dealing with breach-scenario threat models.

### Offline Deadline Enforcement
Don't trust the client for deadline enforcement. Cache the deadline locally for UX (disable button early), but always have the server return 409 on late sync. The server is authoritative.

### Audit Log Immutability
Give the app DB role no `UPDATE`/`DELETE` on the `audit_log` table at the DB level. Don't rely on application-layer enforcement. PostgreSQL role grants are the right tool.
