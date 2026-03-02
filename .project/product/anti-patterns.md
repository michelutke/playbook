---
template: anti-patterns
version: 0.1.0
---
# Anti-Patterns: Playbook

## Architecture Anti-Patterns
- Putting business logic in UI layer (Composables / Views) — keep it in shared KMP modules
- Skipping the shared KMP layer and duplicating logic per platform
- Tight coupling between backend API shape and UI models (use domain models as buffer)
- **Excessive platform-specific iOS code** — custom Swift/UIKit interop should be a last resort; keep as much UI and logic as possible in shared Compose Multiplatform code. Every `expect/actual` or `UIViewControllerRepresentable` must be justified.

## Code Anti-Patterns
- Force-unwrapping / unchecked casts without justification
- Hardcoded strings for user-facing text (use resource system for i18n from day 1)
- Mixing coroutine scopes carelessly — define lifecycle-aware scopes explicitly

## Process Anti-Patterns
- Building web UI before ADR-001 (CMP Web vs Svelte) is resolved
- Designing backend API without confirming client requirements first
- Skipping offline-first design decisions late in the cycle

## Lessons Learned
- (populated as project evolves)
