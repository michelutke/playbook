---
template: overview
version: 0.1.0
---
# Project Overview: Playbook

## Description
Playbook is a cross-platform team appointment and attendance management app for sports clubs. It targets clubs participating in official Jugend und Sport (J+S) programs that require Anwesenheitskontrolle. Think Spielerplus.de, built with Kotlin Multiplatform + Compose Multiplatform.

## Repository Structure
- TBD (pending ADR-002: monorepo vs polyrepo decision)

## Key Modules
- `shared/` — KMP shared domain, data, and business logic
- `androidApp/` — Android entry point (Compose)
- `iosApp/` — iOS entry point (CMP; minimal native code)
- `backend/` — Ktor server
- `webApp/` — Web frontend (CMP Web or Svelte — see ADR-001)

## Current Status
- **Phase:** Initialisation / SDD setup
- ADR-001 (web tech) pending research
- ADR-002 (repo structure) pending decision

## Active Features / Specs
- (none yet — project just initialised)
