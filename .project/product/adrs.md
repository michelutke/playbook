---
template: adrs
version: 0.1.0
---
# Architecture Decision Records: Playbook

## ADR-001: Web Frontend — Compose Multiplatform Web vs Svelte

**Date:** 2026-02-27
**Status:** Pending Research
**Context:** The app targets mobile (iOS/Android) + web for all users. CMP Web (Wasm/JS) would allow a single Kotlin/Compose codebase across all platforms. However, CMP web maturity, performance, bundle size, and accessibility may make Svelte + Ktor a better fit.
**Decision:** Requires research before deciding. See `/sdd:2-research` for research task.
**Consequences:**
- If CMP Web: all-Kotlin stack, shared UI code, but possible CMP web limitations
- If Svelte: best web UX/performance, but separate web codebase to maintain

## ADR-002: Monorepo vs Polyrepo

**Date:** 2026-02-27
**Status:** Pending
**Context:** KMP project naturally groups mobile + shared code. Backend (Ktor) and optional Svelte web could live in the same repo or separate repos.
**Decision:** TBD
**Consequences:** TBD
