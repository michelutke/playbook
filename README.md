# teamorg

Sports team management platform. Create clubs, manage teams, invite members, and assign roles, jerseys, and positions — across Android, iOS, and the web.

## Tech Stack

| Layer | Technology |
|---|---|
| Server | Ktor 3.3.3 · Netty · JWT · Exposed ORM · PostgreSQL · Flyway · Koin |
| Shared | Kotlin Multiplatform · Ktor Client · SQLDelight 2 · Kotlinx Serialization |
| Android | Compose Multiplatform 1.10.1 · Navigation3 · Coil3 · Koin |
| iOS | SwiftUI · shared KMP framework |

Kotlin 2.3.10 · JVM 21 · Android API 34–36

## Architecture

```
Server (Ktor + PostgreSQL)
        │  HTTP/REST + JWT
        ▼
Shared (KMP) — domain, repositories, Ktor client, SQLDelight cache
        │
   ┌────┴────┐
   ▼         ▼
composeApp  iosApp
(Android/   (SwiftUI,
 Desktop)    native)
```

## Local Development

### Prerequisites

- JDK 21
- Android SDK (for Android target)
- Xcode 16+ (for iOS target)
- PostgreSQL running locally

### Environment Variables

| Variable | Description |
|---|---|
| `DATABASE_URL` | PostgreSQL JDBC URL, e.g. `jdbc:postgresql://localhost:5432/teamorg` |
| `JWT_SECRET` | Secret string for signing JWT tokens |
| `API_BASE_URL` | Server base URL used by clients (default: `https://api.teamorg.app`) |

### Run

```bash
# Server
./gradlew :server:run

# Android app (debug APK)
./gradlew :composeApp:assembleDebug

# Desktop (JVM)
./gradlew :composeApp:run

# iOS — open in Xcode
open iosApp/iosApp.xcworkspace
```

## Running Tests

```bash
# Server (H2 in-memory DB, no PostgreSQL needed)
./gradlew :server:test

# Shared KMP
./gradlew :shared:allTests

# Android unit tests
./gradlew :composeApp:testDebugUnitTest

# iOS UI tests (requires a simulator UDID)
xcodebuild test \
  -workspace iosApp/iosApp.xcworkspace \
  -scheme iosApp-Workspace \
  -destination "id=$SIMULATOR_UDID"
```

## License

See [LICENSE](./LICENSE).
