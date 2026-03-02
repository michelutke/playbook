---
template: tech-stack
version: 0.1.0
---
# Tech Stack: Playbook

## Platform
- iOS (native via KMP/CMP)
- Android (native via KMP/CMP)
- Web (TBD — see ADR-001)

## Languages & Frameworks
- **Kotlin Multiplatform (KMP)**: shared domain, data, and business logic
- **Compose Multiplatform (CMP)**: UI layer for Android + iOS; web TBD (see ADR-001)
- **Ktor**: backend API (if CMP web is viable); otherwise also serves Svelte web app
- **Svelte** (conditional): web frontend if CMP web is not production-ready enough

## Key Libraries
- TBD (to be defined in tech spec phase)

## Infrastructure
- TBD (backend hosting, database, push notifications provider)

## Tooling
- Gradle (KMP build)
- Android Studio / Xcode
- TBD CI/CD
