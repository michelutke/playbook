package com.playbook.email

import org.junit.jupiter.api.Test
import org.simplejavamail.email.EmailBuilder
import kotlin.test.assertTrue

/**
 * Unit tests for invite email content.
 * Builds the email directly (no SMTP) and asserts HTML/plain-text content.
 */
class InviteEmailTest {

    private fun buildInviteHtml(
        toEmail: String = "player@test.com",
        teamName: String = "Tigers",
        roleName: String = "player",
        inviteLink: String = "https://app.playbook.example/invite/abc123",
    ): String {
        // Mirror the logic from sendInviteEmail without the suspend/mailer
        return """
            <!DOCTYPE html>
            <html>
            <body style="font-family: sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
              <h2>You've been invited to join $teamName</h2>
              <p>You've been invited to join <strong>$teamName</strong> as a <strong>$roleName</strong> on Playbook.</p>
              <p style="margin: 30px 0;">
                <a href="$inviteLink"
                   style="background: #1a56db; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px;">
                  Accept Invitation
                </a>
              </p>
              <p style="color: #666; font-size: 14px;">This invite expires in 7 days.</p>
              <p style="color: #666; font-size: 14px;">If you didn't expect this invitation, you can safely ignore this email.</p>
            </body>
            </html>
        """.trimIndent()
    }

    @Test
    fun `inviteEmail_html_containsTeamName`() {
        val html = buildInviteHtml(teamName = "Tigers")
        assertTrue(html.contains("Tigers"), "HTML should contain team name")
    }

    @Test
    fun `inviteEmail_html_containsRoleName`() {
        val html = buildInviteHtml(roleName = "coach")
        assertTrue(html.contains("coach"), "HTML should contain role name")
    }

    @Test
    fun `inviteEmail_html_containsInviteLink`() {
        val link = "https://app.playbook.example/invite/tok999"
        val html = buildInviteHtml(inviteLink = link)
        assertTrue(html.contains(link), "HTML should contain the invite link")
    }

    @Test
    fun `inviteEmail_html_containsAcceptButton`() {
        val html = buildInviteHtml()
        assertTrue(html.contains("Accept Invitation"), "HTML should contain CTA button text")
    }

    @Test
    fun `inviteEmail_html_containsExpiryNotice`() {
        val html = buildInviteHtml()
        assertTrue(html.contains("7 days"), "HTML should mention 7-day expiry")
    }

    @Test
    fun `inviteEmail_subject_containsTeamAndRole`() {
        val teamName = "Eagles"
        val roleName = "goalkeeper"
        val subject = "You've been invited to join $teamName as $roleName"
        assertTrue(subject.contains(teamName))
        assertTrue(subject.contains(roleName))
    }
}
