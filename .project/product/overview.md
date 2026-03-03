---
template: overview
version: 0.1.0
---
# Project Overview: Playbook

## Description
Playbook is a cross-platform team appointment and attendance management app for sports clubs. It targets clubs participating in official Jugend und Sport (J+S) programs that require Anwesenheitskontrolle. Think Spielerplus.de, built with Kotlin Multiplatform + Compose Multiplatform.

## Repository Structure (Monorepo — ADR-002)

```
/
├── shared/       — KMP shared domain, data, business logic, SQLDelight cache
├── androidApp/   — Android entry point (Compose Multiplatform)
├── iosApp/       — iOS entry point (CMP; minimal native code)
├── backend/      — Ktor JVM server + Flyway migrations
└── admin/        — SvelteKit SuperAdmin panel
```

## Current Status

**Phase:** Feature-complete (MVP features shipped)

| Feature | Status | Tasks |
|---|---|---|
| team-management | ✅ DONE | 65/65 |
| event-scheduling | ✅ DONE | 36/36 |
| attendance-tracking | ✅ DONE | 35/35 |
| notifications | ✅ DONE (2 manual iOS steps) | 58/58 |
| super-admin | ✅ DONE | 52/52 |
| compose-multiplatform-migration | 🔲 Pending | not yet spec'd |

### Remaining Manual Steps
- `iosApp/README.md`: NT-011 (Xcode Push Notification capability), NT-016 (OneSignal App ID env var)

## Open ADRs
- **ADR-001 (main app web)**: main app web client tech deferred — SA panel uses SvelteKit, main app web TBD

## Archived Specs
Completed feature specs archived at `.project/specs/_archive/`:
- `team-management/`
- `event-scheduling/`
- `attendance-tracking/`
- `notifications/`
- `super-admin/`

Each archive contains a `handover.md` with decisions, key files, migrations, and known limitations.
