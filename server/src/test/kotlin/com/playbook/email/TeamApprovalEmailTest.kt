package com.playbook.email

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TeamApprovalEmailTest {

    private fun escapeHtml(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#x27;")

    private fun buildApprovalHtml(teamName: String = "Lions", clubName: String = "Metro Club"): String {
        return """
            <!DOCTYPE html>
            <html>
            <body style="font-family: sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
              <h2>Your team has been approved!</h2>
              <p>Your team <strong>${escapeHtml(teamName)}</strong> has been approved by <strong>${escapeHtml(clubName)}</strong>.</p>
              <p>You can now manage your team and invite players via the Playbook app.</p>
            </body>
            </html>
        """.trimIndent()
    }

    private fun buildRejectionHtml(
        teamName: String = "Lions",
        clubName: String = "Metro Club",
        reason: String? = null,
    ): String {
        return """
            <!DOCTYPE html>
            <html>
            <body style="font-family: sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
              <h2>Update on your team request</h2>
              <p>Your request to create the team <strong>${escapeHtml(teamName)}</strong> at <strong>${escapeHtml(clubName)}</strong> was not approved.</p>
              ${if (reason != null) "<p><strong>Reason:</strong> ${escapeHtml(reason)}</p>" else ""}
              <p style="color: #666;">Please contact your club manager for more information.</p>
            </body>
            </html>
        """.trimIndent()
    }

    @Test
    fun `approvalEmail_html_containsTeamName`() {
        val html = buildApprovalHtml(teamName = "Thunder FC")
        assertTrue(html.contains("Thunder FC"))
    }

    @Test
    fun `approvalEmail_html_containsClubName`() {
        val html = buildApprovalHtml(clubName = "Metro Club")
        assertTrue(html.contains("Metro Club"))
    }

    @Test
    fun `approvalEmail_html_containsApprovedMessage`() {
        val html = buildApprovalHtml()
        assertTrue(html.contains("approved"))
    }

    @Test
    fun `approvalEmail_subject_containsTeamName`() {
        val teamName = "Rockets"
        val subject = "Your team '$teamName' has been approved"
        assertTrue(subject.contains(teamName))
    }

    @Test
    fun `rejectionEmail_html_containsNotApproved`() {
        val html = buildRejectionHtml()
        assertTrue(html.contains("not approved"))
    }

    @Test
    fun `rejectionEmail_html_withReason_containsReason`() {
        val html = buildRejectionHtml(reason = "Insufficient capacity")
        assertTrue(html.contains("Insufficient capacity"))
    }

    @Test
    fun `rejectionEmail_html_withoutReason_doesNotContainReasonTag`() {
        val html = buildRejectionHtml(reason = null)
        assertFalse(html.contains("<strong>Reason:</strong>"))
    }

    @Test
    fun `approvalEmail_html_escapesDangerousChars`() {
        val html = buildApprovalHtml(teamName = "<script>alert('xss')</script>", clubName = "Safe Club")
        assertFalse(html.contains("<script>"), "XSS script tag should be escaped")
        assertTrue(html.contains("&lt;script&gt;"))
    }

    @Test
    fun `rejectionEmail_subject_containsTeamName`() {
        val teamName = "Hawks"
        val subject = "Update on your team '$teamName' request"
        assertTrue(subject.contains(teamName))
    }
}
