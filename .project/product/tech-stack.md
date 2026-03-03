---
template: tech-stack
version: 0.1.0
---
# Tech Stack: Playbook

## Platform
- Android (Compose Multiplatform)
- iOS (Compose Multiplatform; minimal native code)
- Web — SuperAdmin: SvelteKit; main app web: deferred (see ADR-001)

## Languages & Frameworks

| Layer | Technology |
|---|---|
| Shared domain + data | Kotlin Multiplatform (KMP) |
| Mobile UI | Compose Multiplatform 1.7.1 |
| Backend | Ktor 3.1.0 (JVM) |
| SuperAdmin web | SvelteKit (admin/) |
| Language | Kotlin 2.1.10 |

## Key Libraries

### Backend (JVM)
| Library | Version | Purpose |
|---|---|---|
| Ktor server | 3.1.0 | HTTP framework |
| Koin (`koin-ktor`) | 4.0.0 | Dependency injection |
| Exposed (DSL) | 0.54.0 | Database ORM |
| HikariCP | 5.1.0 | Connection pool |
| Flyway | 10.18.0 | DB migrations |
| PostgreSQL JDBC | 42.7.3 | DB driver |
| Simple Java Mail | 8.3.4 | SMTP email delivery |
| kotlinx-serialization | 1.7.2 | JSON serialisation |

### KMP Shared
| Library | Version | Purpose |
|---|---|---|
| Koin core | 4.0.0 | DI (shared module) |
| SQLDelight | 2.0.2 | Local cache (offline support) |
| kotlinx-datetime | 0.6.0 | Timezone-aware date handling |
| kotlinx-serialization | 1.7.2 | JSON (shared) |

### Android
| Library | Version | Purpose |
|---|---|---|
| AGP | 8.5.0 | Android Gradle plugin |
| Compose Multiplatform | 1.7.1 | UI |

## Infrastructure

| Concern | Solution |
|---|---|
| Database | PostgreSQL |
| Push notifications | OneSignal (abstracted via `PushService`) |
| File storage | Self-hosted filesystem (logo uploads) |
| Email | Self-hosted / internal SMTP (Simple Java Mail) |
| CI/CD | TBD |
| Hosting | TBD |

## Tooling
- Gradle (KMP + Android build)
- Android Studio / Xcode
- Flyway (DB migrations — versioned SQL files)
