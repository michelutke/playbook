package com.playbook.e2e

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.playbook.PlaybookApp
import com.playbook.auth.AuthViewModel
import com.playbook.data.network.HttpClientFactory
import com.playbook.data.repository.AuthRepositoryImpl
import com.playbook.preferences.UserPreferences
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Rule
import org.junit.Test
import org.koin.compose.KoinContext
import org.koin.dsl.module
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.junit.After
import org.junit.Before
import androidx.test.platform.app.InstrumentationRegistry
import com.playbook.domain.AuthResponse
import com.playbook.domain.AuthUser
import com.playbook.ui.emptystate.EmptyStateViewModel
import com.playbook.ui.login.LoginViewModel
import com.playbook.ui.register.RegisterViewModel
import io.ktor.client.plugins.DefaultRequest

class AuthE2ETest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setup() {
        stopKoin()
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun testRegistrationToEmptyState() {
        val mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/auth/register" -> {
                    respond(
                        content = Json.encodeToString(
                            AuthResponse(
                                token = "fake-token",
                                userId = "user-123",
                                displayName = "Test User",
                                avatarUrl = null
                            )
                        ),
                        status = HttpStatusCode.Created,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    )
                }
                "/auth/me" -> {
                    respond(
                        content = Json.encodeToString(
                            AuthUser(
                                userId = "user-123",
                                email = "test@example.com",
                                displayName = "Test User",
                                avatarUrl = null,
                                isSuperAdmin = false
                            )
                        ),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    )
                }
                else -> error("Unhandled path: ${request.url.encodedPath}")
            }
        }

        val testModule = module {
            single { UserPreferences(InstrumentationRegistry.getInstrumentation().targetContext) }
            single {
                HttpClient(mockEngine) {
                    install(ContentNegotiation) {
                        json(Json { ignoreUnknownKeys = true })
                    }
                    install(DefaultRequest) {
                        url("http://localhost")
                    }
                }
            }
            single { AuthRepositoryImpl(get(), get()) }
            factory { AuthViewModel(get()) }
            factory { params -> LoginViewModel(get(), onLoginSuccess = params.get()) }
            factory { params -> RegisterViewModel(get(), onRegisterSuccess = params.get()) }
            factory { EmptyStateViewModel(get()) }
        }

        startKoin {
            modules(testModule)
        }

        composeTestRule.setContent {
            KoinContext {
                PlaybookApp()
            }
        }

        // 1. Start at Login, navigate to Register
        composeTestRule.onNodeWithText("Don't have an account? Create one").performClick()

        // 2. Fill registration details
        composeTestRule.onNodeWithText("Display Name").performTextInput("Test User")
        composeTestRule.onNodeWithText("Email").performTextInput("test@example.com")
        composeTestRule.onNodeWithText("Password").performTextInput("Password123!")
        composeTestRule.onNodeWithText("Confirm Password").performTextInput("Password123!")

        // 3. Click Register
        composeTestRule.onNodeWithText("Create Account", useUnmergedTree = true).performClick()

        // 4. Verify redirect to Empty State
        composeTestRule.onNodeWithText("Welcome to Playbook").assertIsDisplayed()
        composeTestRule.onNodeWithText("You're not part of a team yet.").assertIsDisplayed()
    }

    @Test
    fun testLoginToMainApp() {
        val mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/auth/login" -> {
                    respond(
                        content = Json.encodeToString(
                            AuthResponse(
                                token = "fake-token",
                                userId = "user-123",
                                displayName = "Test User",
                                avatarUrl = null
                            )
                        ),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    )
                }
                "/auth/me" -> {
                    respond(
                        content = Json.encodeToString(
                            AuthUser(
                                userId = "user-123",
                                email = "test@example.com",
                                displayName = "Test User",
                                avatarUrl = null,
                                isSuperAdmin = false
                            )
                        ),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    )
                }
                else -> error("Unhandled path: ${request.url.encodedPath}")
            }
        }

        val testModule = module {
            single { UserPreferences(InstrumentationRegistry.getInstrumentation().targetContext) }
            single {
                HttpClient(mockEngine) {
                    install(ContentNegotiation) {
                        json(Json { ignoreUnknownKeys = true })
                    }
                    install(DefaultRequest) {
                        url("http://localhost")
                    }
                }
            }
            single { AuthRepositoryImpl(get(), get()) }
            factory { AuthViewModel(get()) }
            factory { params -> LoginViewModel(get(), onLoginSuccess = params.get()) }
            factory { params -> RegisterViewModel(get(), onRegisterSuccess = params.get()) }
            factory { EmptyStateViewModel(get()) }
        }

        startKoin {
            modules(testModule)
        }

        composeTestRule.setContent {
            KoinContext {
                PlaybookApp()
            }
        }

        // 1. Fill login details
        composeTestRule.onNodeWithText("Email").performTextInput("test@example.com")
        composeTestRule.onNodeWithText("Password").performTextInput("Password123!")

        // 2. Click Login
        composeTestRule.onNodeWithText("Sign in", useUnmergedTree = true).performClick()

        // 3. Verify redirect to Empty State (since user has no team yet)
        composeTestRule.onNodeWithText("Welcome to Playbook").assertIsDisplayed()
    }
}
