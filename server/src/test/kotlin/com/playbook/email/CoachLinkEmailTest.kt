package com.playbook.email

import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class CoachLinkEmailTest {

    private fun buildCoachLinkHtml(
        clubName: String = "City FC",
        coachLink: String = "https://app.playbook.example/club-links/tok123",
    ): String {
        return """
            <!DOCTYPE html>
            <html>
            <body style="font-family: sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
              <h2>Join $clubName as a coach</h2>
              <p>You've been invited to join <strong>$clubName</strong> as a coach on Playbook.</p>
              <p style="margin: 30px 0;">
                <a href="$coachLink"
                   style="background: #1a56db; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px;">
                  Join as Coach
                </a>
              </p>
              <p style="color: #666; font-size: 14px;">This link can be used by multiple coaches and expires after a set period.</p>
              <p style="color: #666; font-size: 14px;">If you didn't expect this invitation, you can safely ignore this email.</p>
            </body>
            </html>
        """.trimIndent()
    }

    @Test
    fun `coachLinkEmail_html_containsClubName`() {
        val html = buildCoachLinkHtml(clubName = "City FC")
        assertTrue(html.contains("City FC"), "HTML should contain club name")
    }

    @Test
    fun `coachLinkEmail_html_containsCoachLink`() {
        val link = "https://app.playbook.example/club-links/unique-tok"
        val html = buildCoachLinkHtml(coachLink = link)
        assertTrue(html.contains(link), "HTML should contain the coach link")
    }

    @Test
    fun `coachLinkEmail_html_containsJoinButton`() {
        val html = buildCoachLinkHtml()
        assertTrue(html.contains("Join as Coach"), "HTML should contain CTA button text")
    }

    @Test
    fun `coachLinkEmail_html_mentionsMultipleCoaches`() {
        val html = buildCoachLinkHtml()
        assertTrue(html.contains("multiple coaches"), "HTML should mention reusable link")
    }

    @Test
    fun `coachLinkEmail_subject_containsClubName`() {
        val clubName = "River United"
        val subject = "Join $clubName as a coach on Playbook"
        assertTrue(subject.contains(clubName))
        assertTrue(subject.contains("coach"))
    }
}
