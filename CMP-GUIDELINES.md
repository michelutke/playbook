# Compose Multiplatform (CMP) Guidelines

## Core Rule
**Always use proper CMP libraries. Minimize platform-specific workarounds.**

If a library doesn't have CMP support (iOS + Android), find the CMP-compatible alternative — don't hack around it with expect/actual if a real solution exists.

---

## Library Selection

### Navigation
| ❌ Do NOT use | ✅ Use instead |
|---|---|
| `androidx.navigation3:navigation3-ui` (Android-only) | `org.jetbrains.androidx.navigation3:navigation3-ui` |
| `androidx.navigation3:navigation3-runtime` (Android-only NavBackStack) | `org.jetbrains.androidx.navigation3:navigation3-ui` (includes runtime) |
| `androidx.navigation:navigation-compose` (Android-only) | `org.jetbrains.androidx.navigation:navigation-compose` |

**JetBrains Navigation 3 (CMP — iOS + Android + Desktop):**
```toml
# libs.versions.toml
navigation3-jb = "1.0.0+dev2887"
compose-navigation3 = { module = "org.jetbrains.androidx.navigation3:navigation3-ui", version.ref = "navigation3-jb" }
```
```kotlin
// commonMain.dependencies — works on iOS, Android, Desktop
implementation(libs.compose.navigation3)
```
Repo required: `maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")` — already in settings.

**API (verified from sources JAR):**
```kotlin
// backStack is just a MutableList
val backStack = remember { mutableStateListOf<Screen>(Screen.Loading) }

// NavDisplay replaces NavHost
NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeAt(backStack.lastIndex) },
    entryProvider = { screen ->
        NavEntry(screen) { /* @Composable content */ }
    }
)

// Navigate: backStack.add(Screen.Login)
// Pop: backStack.removeAt(backStack.lastIndex)
// popUpTo equivalent: backStack.clear(); backStack.add(Screen.Events)
```
Imports: `androidx.navigation3.runtime.NavEntry`, `androidx.navigation3.ui.NavDisplay`
⚠️ NOT the same as Google nav3 — no NavController, no NavHost, no rememberNavController.

### ViewModel
| ❌ Do NOT use | ✅ Use instead |
|---|---|
| `androidx.lifecycle:lifecycle-viewmodel-compose` (Android-only) | `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose` |

### Lifecycle
| ❌ Do NOT use | ✅ Use instead |
|---|---|
| `androidx.lifecycle:lifecycle-*` (Android-only) | `org.jetbrains.androidx.lifecycle:lifecycle-*` |

### Image Loading
| ❌ Do NOT use | ✅ Use instead |
|---|---|
| Coil2 (`io.coil-kt:coil-compose`) | Coil3 (`io.coil-kt.coil3:coil-compose`) — has CMP support |
| Glide | Coil3 or Kamel |

### Settings / Preferences
| ❌ Do NOT use | ✅ Use instead |
|---|---|
| Android `SharedPreferences` directly | `com.russhwolf:multiplatform-settings` (package: `com.russhwolf.settings.*`) |

### Date/Time
| ❌ Do NOT use | ✅ Use instead |
|---|---|
| `java.time.*` in shared code | `kotlinx-datetime` (PINNED to 0.6.0 — do NOT upgrade to 0.7.x) |

---

## Repository Setup
Always include both in `dependencyResolutionManagement`:
```kotlin
google()
mavenCentral()
maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
maven("https://maven.pkg.jetbrains.space/public/p/compose/interactive")
```

- **Google Maven** → AndroidX, Google libs
- **JetBrains compose/dev** → JetBrains CMP forks of AndroidX libs (`org.jetbrains.androidx.*`)

---

## How to verify CMP support before adding a library

```bash
# Check if a Google AndroidX lib has a JetBrains CMP fork
curl -s "https://maven.pkg.jetbrains.space/public/p/compose/dev/org/jetbrains/androidx/<group>/<artifact>/maven-metadata.xml" | grep '<version>'

# Check if a library has iOS artifacts (KMP)
curl -s "https://dl.google.com/android/maven2/<group>/<artifact>/maven-metadata.xml"
# Then check the .module file for iOS variants:
curl -s "https://...<artifact>-<version>.module" | python3 -c "import sys,json; [print(v['name']) for v in json.load(sys.stdin).get('variants',[])]"
# Look for: iosArm64*, iosSimulatorArm64*, iosX64*
```

If no iOS variants exist → it's Android-only → find CMP alternative.

---

## Source set rules

- `commonMain` → only CMP-compatible libs (verified iOS + Android support)
- `androidMain` → Android-specific integrations only (Koin Android, Activity, etc.)
- `iosMain` → iOS-specific integrations only
- **Never put Android-only libraries in `commonMain`**

---

## Known version pins
- `kotlinx-datetime = "0.6.0"` — DO NOT upgrade (0.7.x = Kotlin/Native IR crash)
- `navigation3 = "1.0.1"` — use `navigation3-runtime` in commonMain only; NavHost needs CMP version
