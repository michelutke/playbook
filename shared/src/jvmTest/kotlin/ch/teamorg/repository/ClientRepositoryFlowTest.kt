package ch.teamorg.repository

import ch.teamorg.data.repository.AuthRepositoryImpl
import ch.teamorg.data.repository.ClubRepositoryImpl
import ch.teamorg.data.repository.InviteRepositoryImpl
import ch.teamorg.data.repository.TeamRepositoryImpl
import ch.teamorg.domain.AuthResponse
import ch.teamorg.domain.RegisterRequest
import ch.teamorg.module as serverModule
import ch.teamorg.preferences.UserPreferences
import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.engine.applicationEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.AfterClass
import org.junit.BeforeClass
import org.testcontainers.containers.PostgreSQLContainer
import java.net.ServerSocket
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests for shared client repository implementations.
 *
 * Starts a real Ktor/Netty server on a random free TCP port, backed by a Testcontainers
 * PostgreSQL instance. The tests instantiate the actual [TeamRepositoryImpl],
 * [ClubRepositoryImpl], [InviteRepositoryImpl] and [AuthRepositoryImpl] from the shared module
 * and exercise the real HTTP client code path — including Content-Type headers, body
 * serialization, and response deserialization.
 */
class ClientRepositoryFlowTest {

    companion object {

        private val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:15-alpine")
            .withDatabaseName("teamorg_test_client")
            .withUsername("test")
            .withPassword("test")

        private var serverPort: Int = 0
        private lateinit var server: io.ktor.server.engine.EmbeddedServer<*, *>

        @JvmStatic
        @BeforeClass
        fun startServer() {
            postgres.start()
            serverPort = findFreePort()

            val appConfig = MapApplicationConfig(
                "jwt.secret" to "test_secret_32_chars_long_minimum_required",
                "jwt.issuer" to "teamorg",
                "jwt.audience" to "teamorg-users",
                "jwt.realm" to "teamorg",
                "jwt.expiry-days" to "30",
                "database.url" to postgres.jdbcUrl,
                "database.driver" to "org.postgresql.Driver",
                "database.username" to postgres.username,
                "database.password" to postgres.password
            )

            val env = applicationEnvironment { config = appConfig }

            server = embeddedServer(
                factory = Netty,
                environment = env,
                configure = { connector { port = serverPort } },
                module = { serverModule() }
            )

            server.start(wait = false)
            waitForPort(serverPort)
        }

        @JvmStatic
        @AfterClass
        fun stopServer() {
            server.stop(100, 500)
            postgres.stop()
        }

        private fun findFreePort(): Int = ServerSocket(0).use { it.localPort }

        private fun waitForPort(port: Int, retries: Int = 30, delayMs: Long = 200) {
            repeat(retries) {
                try {
                    java.net.Socket("localhost", port).close()
                    return
                } catch (_: Exception) {
                    Thread.sleep(delayMs)
                }
            }
            error("Server did not start on port $port within ${retries * delayMs}ms")
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Test infrastructure
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Builds a Ktor HttpClient configured identically to the shared module's
     * [ch.teamorg.data.network.HttpClientFactory], pointed at the test server.
     * [token] is evaluated per-request via the [DefaultRequest] lambda.
     */
    private fun buildClient(token: () -> String?): HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
        install(DefaultRequest) {
            url("http://localhost:$serverPort")
            contentType(ContentType.Application.Json)
            val t = token()
            if (t != null) header("Authorization", "Bearer $t")
        }
    }

    /**
     * Registers a new user and returns the [AuthResponse].
     *
     * This calls [AuthRepositoryImpl.register] directly — the real shared code path.
     * [UserPreferences] is a final actual class backed by a single JVM preferences node,
     * so we cannot have independent instances per test. Instead we:
     * 1. Create a fresh prefs + unauthenticated client
     * 2. Call register (which stores the token in prefs)
     * 3. Return the [AuthResponse] so the caller can extract the token
     *
     * The caller then builds a *new* client that captures the token directly from the
     * [AuthResponse], bypassing the shared prefs node entirely.
     */
    private suspend fun registerUser(email: String, displayName: String, password: String = "Password1!"): AuthResponse {
        val prefs = UserPreferences()
        prefs.clearToken()
        val unauthClient = buildClient { null }
        return AuthRepositoryImpl(unauthClient, prefs)
            .register(RegisterRequest(email, password, displayName))
            .getOrThrow()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Flow 2: register → create club → create team → createInvite → redeem → roster
    // Tests that createInvite sends a correct Content-Type header (the original bug).
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `flow2 createInvite sends correct Content-Type and roster is updated after redeem`() = runBlocking {
        // Register CM and build an authenticated client with its token
        val cmAuth = registerUser("client.flow2.cm@test.local", "CM ClientFlow2")
        val cmClient = buildClient { cmAuth.token }

        // Wire up the shared repositories to the CM client
        val cmClubRepo = ClubRepositoryImpl(cmClient)
        val cmTeamRepo = TeamRepositoryImpl(cmClient)

        val club = cmClubRepo.createClub("Client Flow2 Club", "volleyball", "Bern").getOrThrow()
        assertNotNull(club.id)

        val team = cmClubRepo.createTeam(club.id, "Client Flow2 Team", null).getOrThrow()
        assertNotNull(team.id)

        // Critical: exercises real HTTP POST with JSON body + Content-Type header.
        // A missing Content-Type would cause the server to return 415 Unsupported Media Type,
        // which makes getOrThrow() throw and the test fail.
        val inviteUrl = cmTeamRepo.createInvite(team.id, "player", null).getOrThrow()
        assertNotNull(inviteUrl)
        val inviteToken = inviteUrl.substringAfterLast("/")
        assertTrue(inviteToken.isNotBlank(), "invite token must be non-blank")

        // Register player and build an authenticated client with its token
        val playerAuth = registerUser("client.flow2.player@test.local", "Player ClientFlow2")
        val playerClient = buildClient { playerAuth.token }
        val playerInviteRepo = InviteRepositoryImpl(playerClient)

        // Get invite details via InviteRepositoryImpl
        val inviteDetails = playerInviteRepo.getInviteDetails(inviteToken).getOrThrow()
        assertFalse(inviteDetails.alreadyRedeemed)
        assertTrue(inviteDetails.teamName.isNotBlank())
        assertTrue(inviteDetails.clubName.isNotBlank())

        // Redeem invite via InviteRepositoryImpl
        playerInviteRepo.redeemInvite(inviteToken).getOrThrow()

        // Verify roster via TeamRepositoryImpl (CM perspective)
        val roster = cmTeamRepo.getTeamRoster(team.id).getOrThrow()
        val playerEntry = roster.find { it.userId == playerAuth.userId }
        assertNotNull(playerEntry, "Player must appear in roster after redeem")
        kotlin.test.assertEquals("player", playerEntry.role)
        assertNull(playerEntry.jerseyNumber, "jerseyNumber must be null initially")
        assertNull(playerEntry.position, "position must be null initially")
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Auth: register + isLoggedIn
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `auth register saves token and AuthRepositoryImpl reports isLoggedIn`() = runBlocking {
        val prefs = UserPreferences()
        prefs.clearToken()
        val unauthClient = buildClient { null }
        val authRepo = AuthRepositoryImpl(unauthClient, prefs)

        val result = authRepo.register(
            RegisterRequest("client.flow3@test.local", "Password1!", "Flow3 User")
        )
        assertTrue(result.isSuccess)
        val authResponse = result.getOrThrow()
        assertNotNull(authResponse.token)
        assertNotNull(authResponse.userId)

        // AuthRepositoryImpl must have saved the token to prefs
        assertTrue(authRepo.isLoggedIn())
        assertTrue(prefs.getToken() != null)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Roles: getMyRoles after club creation
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `getMyRoles returns club_manager role after creating a club`() = runBlocking {
        val cmAuth = registerUser("client.flow4.cm@test.local", "CM ClientFlow4")
        val cmClient = buildClient { cmAuth.token }
        val clubRepo = ClubRepositoryImpl(cmClient)
        val teamRepo = TeamRepositoryImpl(cmClient)

        val club = clubRepo.createClub("Client Flow4 Club", "football", null).getOrThrow()

        val roles = teamRepo.getMyRoles().getOrThrow()
        assertTrue(
            roles.clubRoles.any { it.clubId == club.id && it.role == "club_manager" },
            "clubRoles must contain club_manager entry for club ${club.id}"
        )
    }

    // ──────────────────────────────────────────────────────────────────────────
    // removeMember: CM removes player from team roster
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `removeMember removes player from team roster`() = runBlocking {
        val cmAuth = registerUser("client.flow5.cm@test.local", "CM ClientFlow5")
        val cmClient = buildClient { cmAuth.token }
        val cmClubRepo = ClubRepositoryImpl(cmClient)
        val cmTeamRepo = TeamRepositoryImpl(cmClient)

        val club = cmClubRepo.createClub("Client Flow5 Club", "volleyball", null).getOrThrow()
        val team = cmClubRepo.createTeam(club.id, "Client Flow5 Team", null).getOrThrow()
        val inviteUrl = cmTeamRepo.createInvite(team.id, "player", null).getOrThrow()
        val inviteToken = inviteUrl.substringAfterLast("/")

        val playerAuth = registerUser("client.flow5.player@test.local", "Player ClientFlow5")
        val playerClient = buildClient { playerAuth.token }
        InviteRepositoryImpl(playerClient).redeemInvite(inviteToken).getOrThrow()

        // Assert player is in roster before removal
        assertNotNull(
            cmTeamRepo.getTeamRoster(team.id).getOrThrow().find { it.userId == playerAuth.userId },
            "Player must be in roster before removal"
        )

        // Remove via TeamRepositoryImpl (requires club_manager role)
        cmTeamRepo.removeMember(team.id, playerAuth.userId).getOrThrow()

        // Assert player is gone
        assertNull(
            cmTeamRepo.getTeamRoster(team.id).getOrThrow().find { it.userId == playerAuth.userId },
            "Player must not appear in roster after removal"
        )
    }
}
