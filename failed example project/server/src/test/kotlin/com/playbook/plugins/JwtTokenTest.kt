package com.playbook.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.TokenExpiredException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class JwtTokenTest {

    private val secret = "test-jwt-secret-for-unit-tests-only"
    private val issuer = "playbook"
    private val audience = "playbook-app"
    private val userId = "user-abc-123"

    private fun createTestToken(
        audience: String = this.audience,
        expiresAt: Date = Date(System.currentTimeMillis() + 3_600_000),
    ): String = JWT.create()
        .withIssuer(issuer)
        .withAudience(audience)
        .withSubject(userId)
        .withClaim("sub", userId)
        .withExpiresAt(expiresAt)
        .sign(Algorithm.HMAC256(secret))

    @Test
    fun `jwt_createdWithCorrectIssuer`() {
        val token = createTestToken()
        val decoded = JWT.decode(token)
        assertEquals(issuer, decoded.issuer)
    }

    @Test
    fun `jwt_createdWithCorrectAudience`() {
        val token = createTestToken()
        val decoded = JWT.decode(token)
        assertEquals(listOf(audience), decoded.audience)
    }

    @Test
    fun `jwt_claimsContainSubWithUserId`() {
        val token = createTestToken()
        val decoded = JWT.decode(token)
        assertEquals(userId, decoded.getClaim("sub").asString())
    }

    @Test
    fun `jwt_verifier_acceptsValidToken`() {
        val token = createTestToken()
        val verifier = JWT.require(Algorithm.HMAC256(secret))
            .withIssuer(issuer)
            .withAudience(audience)
            .build()
        val decoded = verifier.verify(token)
        assertNotNull(decoded)
        assertEquals(userId, decoded.getClaim("sub").asString())
    }

    @Test
    fun `jwt_expiredToken_failsVerification`() {
        val expiredToken = createTestToken(
            expiresAt = Date(System.currentTimeMillis() - 1000)
        )
        val verifier = JWT.require(Algorithm.HMAC256(secret))
            .withIssuer(issuer)
            .withAudience(audience)
            .build()
        assertThrows<TokenExpiredException> {
            verifier.verify(expiredToken)
        }
    }

    @Test
    fun `jwt_wrongAudience_failsVerification`() {
        val token = createTestToken(audience = "playbook-sa")
        val verifier = JWT.require(Algorithm.HMAC256(secret))
            .withIssuer(issuer)
            .withAudience("playbook-app")
            .build()
        assertThrows<Exception> {
            verifier.verify(token)
        }
    }

    @Test
    fun `jwt_wrongSecret_failsVerification`() {
        val token = createTestToken()
        val verifier = JWT.require(Algorithm.HMAC256("wrong-secret"))
            .withIssuer(issuer)
            .withAudience(audience)
            .build()
        assertThrows<Exception> {
            verifier.verify(token)
        }
    }

    @Test
    fun `jwt_saAudienceToken_distinguishableFromRegularToken`() {
        val regularToken = createTestToken(audience = "playbook-app")
        val saToken = createTestToken(audience = "playbook-sa")
        val decodedRegular = JWT.decode(regularToken)
        val decodedSa = JWT.decode(saToken)
        assertEquals(listOf("playbook-app"), decodedRegular.audience)
        assertEquals(listOf("playbook-sa"), decodedSa.audience)
    }
}
