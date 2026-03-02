---
template: research
version: 0.1.0
---
# Research: Team Management

> **Note:** All web-fetch tools (WebSearch, WebFetch, Firecrawl, Context7) were unavailable during this session. Findings are from model training data (knowledge cutoff Aug 2025). Verify versions against official docs before implementation.

---

## Ktor Multipart File Upload

- **Version**: 3.1.x (Ktor 3.x is the current stable line as of mid-2025; verify at https://ktor.io/changelog/)
- **Docs**: https://ktor.io/docs/server-uploads.html  /  https://ktor.io/docs/server-static-content.html
- **KMP support**: N/A — server-side multipart is Ktor server (JVM/Native); not a KMP shared concern
- **Key findings**:
  - No extra dependency needed — multipart is built into `io.ktor:ktor-server-core`
  - Receive multipart in a route handler:
    ```kotlin
    post("/clubs/{id}/logo") {
        val multipart = call.receiveMultipart()
        multipart.forEachPart { part ->
            if (part is PartData.FileItem) {
                val bytes = part.streamProvider().readBytes()
                File("uploads/${part.originalFileName}").writeBytes(bytes)
            }
            part.dispose()
        }
        call.respond(HttpStatusCode.OK)
    }
    ```
  - Per-field size limits (Ktor 2.3+/3.x): `call.receiveMultipart(fileFieldLimit = 5 * 1024 * 1024L)` — verify exact param name in current docs
  - No built-in global file size limit by default — must be set explicitly (via plugin or reverse proxy) to prevent OOM
  - Serving uploaded files from filesystem (Ktor 2.2+):
    ```kotlin
    routing {
        staticFiles("/uploads", File("uploads"))
    }
    ```
    `staticFiles(remotePath, dir)` replaced deprecated `staticRootFolder` + `files()` API
  - Content-type validation is manual — check `part.contentType` against allowed image MIME types (`image/png`, `image/jpeg`, `image/webp`)
  - Logo replacement strategy: overwrite at same path or delete-then-write; update `logo_url` in DB in the same request handler
- **Decision**: use — built-in, zero extra dependency; use `staticFiles()` for serving

---

## Secure Token Generation (Kotlin/JVM)

- **Version**: N/A — JDK standard library (`java.security.SecureRandom`, `java.util.Base64`); no external dependency
- **Docs**: https://docs.oracle.com/en/java/docs/java.base/java/security/SecureRandom.html
- **KMP support**: No (JVM-only API) — token generation belongs in the Ktor server module, not KMP shared domain
- **Key findings**:
  - Recommended pattern — `SecureRandom` + URL-safe Base64:
    ```kotlin
    import java.security.SecureRandom
    import java.util.Base64

    fun generateToken(byteLength: Int = 32): String {
        val bytes = ByteArray(byteLength)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
    ```
  - 32 bytes → 43-char URL-safe string; ~256-bit entropy; safe for invite tokens and coach links
  - `UUID.randomUUID()` is UUID v4 (122-bit); structurally predictable (version/variant bits leak info); acceptable but SecureRandom is strictly better for security tokens
  - HMAC-signed tokens: useful for stateless validation without a DB lookup. For this use case (tokens validated against DB, single-use or revocable), opaque random tokens are simpler and equally secure — the DB is authoritative
  - Invite tokens: store raw token in `invites.invite_token` (unique index); for higher security, store `SHA-256(token)` and compare hash on lookup (defense-in-depth, mirrors password-reset best practice)
  - Coach links: same approach; `club_coach_links.token` — rotate by setting `revoked_at` on old row, insert new row with fresh token
  - Token generation is server-side infrastructure — keep out of KMP shared module
- **Decision**: use `SecureRandom` + URL-safe Base64 — idiomatic, zero dependencies, sufficient entropy; no HMAC needed

---

## Kotlin Email Sending — Simple Java Mail

- **Version**: 8.3.x (latest stable as of mid-2024; verify at https://www.simplejavamail.org/ or https://mvnrepository.com/artifact/org.simplejavamail/simple-java-mail)
- **Docs**: https://www.simplejavamail.org/
- **KMP support**: No — JVM-only (built on Jakarta Mail stack)
- **Key findings**:
  - Gradle artifact: `org.simplejavamail:simple-java-mail:8.x.x`
  - Fluent API, Kotlin-friendly, most mature Java SMTP library; active maintenance
  - Basic usage:
    ```kotlin
    val mailer: Mailer = MailerBuilder
        .withSMTPServer(smtpHost, smtpPort, smtpUser, smtpPass)
        .withTransportStrategy(TransportStrategy.SMTP_TLS) // port 587, STARTTLS
        .buildMailer()

    val email = EmailBuilder.startingBlank()
        .from("noreply@yourapp.com")
        .to(recipientEmail)
        .withSubject("You've been invited to join a team")
        .withPlainText("Accept here: https://app.example.com/invites/$token")
        .buildEmail()

    // Wrap blocking call in IO dispatcher for Ktor coroutine context
    withContext(Dispatchers.IO) { mailer.sendMail(email) }
    ```
  - Register `Mailer` as a Koin `single { }` — it manages its own SMTP connection pool internally
  - Use `TransportStrategy.SMTP_TLS` (STARTTLS, port 587) for own SMTP server; or `SMTPS` (port 465, SSL)
  - Coroutines: `sendMail()` is blocking; wrap with `withContext(Dispatchers.IO)`. The `sendMail(email, async = true)` variant uses an internal thread pool — prefer `Dispatchers.IO` for uniform coroutine management
  - Config via env vars: `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASS`
  - **Alternative — kotlin-mail** (`com.github.vanniktech:kotlin-mail`, ~0.4.x): Kotlin-first with suspend functions; KMP-targeted but JVM SMTP delivery only; smaller community, sparse docs — not recommended for production yet
  - **Alternative — Jakarta Mail** (raw): low-level, verbose; no benefit over Simple Java Mail for this use case
- **Decision**: use Simple Java Mail — mature, well-documented, Kotlin-friendly; wrap sends in `Dispatchers.IO`

---

## Koin DI (KMP + Ktor)

- **Version**: 4.0.x (Koin 4.0 released late 2024; verify at https://insert-koin.io/docs/setup/koin)
- **Docs**: https://insert-koin.io/docs/reference/koin-ktor/ktor
- **KMP support**: Yes — `koin-core` supports JVM, Android, iOS, JS, Native; `koin-ktor` is JVM-only (server plugin)
- **Key findings**:
  - Gradle artifacts:
    ```kotlin
    // KMP shared module (commonMain)
    implementation("io.insert-koin:koin-core:4.0.x")

    // Ktor server module (jvmMain)
    implementation("io.insert-koin:koin-ktor:4.0.x")
    implementation("io.insert-koin:koin-logger-slf4j:4.0.x")
    ```
  - Install Koin as a Ktor plugin:
    ```kotlin
    fun Application.configureKoin() {
        install(Koin) {
            slf4jLogger()
            modules(sharedModule, serverModule)
        }
    }
    ```
  - Recommended module split:
    ```kotlin
    // shared/commonMain — domain + repository bindings
    val sharedModule = module {
        single<ClubRepository> { ClubRepositoryImpl(get()) }
        single<TeamRepository> { TeamRepositoryImpl(get()) }
        single<InviteRepository> { InviteRepositoryImpl(get()) }
    }

    // server/jvmMain — infrastructure (DB, mailer, file storage)
    val serverModule = module {
        single { DatabaseFactory.create() }         // Exposed/HikariCP
        single { MailerFactory.create() }           // Simple Java Mail Mailer
    }
    ```
  - Inject in Ktor routes:
    ```kotlin
    // via extension (koin-ktor provides inject() delegate inside Application/Route scope)
    val clubRepo: ClubRepository by inject()

    // or explicit get
    val clubRepo = call.application.getKoin().get<ClubRepository>()
    ```
  - Scoping: `single { }` for repositories, mailer, DB pool (singletons); `factory { }` for per-request objects if needed
  - Koin 4.x: `startKoin {}` still works for non-Ktor entry points (tests, CLI); the Ktor plugin is the preferred path in server context
  - Testing: `io.insert-koin:koin-test:4.0.x` — provides `KoinTest`, `declareMock<T>()`, `checkModules()` for module validation
  - No known breaking issues between `koin-core` (shared) and `koin-ktor` (server) in 4.x
- **Decision**: use — first-class KMP + Ktor support, clean module composition, active project; `koin-ktor` 4.x is the current recommended integration path
