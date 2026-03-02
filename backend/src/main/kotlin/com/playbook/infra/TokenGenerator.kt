package com.playbook.infra

import java.security.SecureRandom
import java.util.Base64

/**
 * TM-031: Generate a URL-safe secure random token.
 * 32 bytes = 43-char Base64-URL string, ~256-bit entropy.
 */
fun generateToken(byteLength: Int = 32): String {
    val bytes = ByteArray(byteLength)
    SecureRandom().nextBytes(bytes)
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}
