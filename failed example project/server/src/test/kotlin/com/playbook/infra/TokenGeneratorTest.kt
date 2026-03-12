package com.playbook.infra

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TokenGeneratorTest {

    @Test
    fun `generateToken_returns43CharUrlSafeString`() {
        val token = generateToken()
        assertEquals(43, token.length, "Expected 43 chars (32 bytes Base64URL without padding), got ${token.length}")
    }

    @Test
    fun `generateToken_producesUniqueValuesAcross100Calls`() {
        val tokens = (1..100).map { generateToken() }.toSet()
        assertEquals(100, tokens.size, "Expected 100 unique tokens but got duplicates")
    }

    @Test
    fun `generateToken_containsNoPaddingChars`() {
        val token = generateToken()
        assertFalse(token.contains('='), "Token should not contain Base64 padding char '='")
    }

    @Test
    fun `generateToken_containsOnlyUrlSafeChars`() {
        val token = generateToken()
        val validChars = Regex("[A-Za-z0-9_-]+")
        assertTrue(validChars.matches(token), "Token contains non-URL-safe characters: $token")
    }

    @Test
    fun `generateToken_customByteLength_producesCorrectLength`() {
        // 16 bytes → ceil(16 * 4/3) without padding = 22 chars
        val token = generateToken(byteLength = 16)
        assertEquals(22, token.length)
    }

    @Test
    fun `generateToken_defaultLength_uses32Bytes`() {
        // Verify 32 bytes without padding = 43 chars
        val token = generateToken(byteLength = 32)
        assertEquals(43, token.length)
    }
}
