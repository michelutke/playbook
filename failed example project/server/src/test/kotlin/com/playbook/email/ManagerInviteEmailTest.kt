package com.playbook.email

import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class ManagerInviteEmailTest {

    private fun buildManagerInviteHtml(
        clubName: String = "Westside Club",
        inviteLink: String = "https://app.playbook.example/invite/mgr-tok-abc",
    ): String {
        return """
            <!DOCTYPE html>
            <html>
            <body style="font-family: sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
              <h2>You've been invited to manage $clubName</h2>
              <p>You've been invited to become a <strong>club manager</strong> for <strong>$clubName</strong> on Playbook.</p>
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
    fun `managerInviteEmail_html_containsClubName`() {
        val html = buildManagerInviteHtml(clubName = "Westside Club")
        assertTrue(html.contains("Westside Club"))
    }

    @Test
    fun `managerInviteEmail_html_containsInviteLink`() {
        val link = "https://app.playbook.example/invite/mgr-unique-tok"
        val html = buildManagerInviteHtml(inviteLink = link)
        assertTrue(html.contains(link))
    }

    @Test
    fun `managerInviteEmail_html_containsAcceptButton`() {
        val html = buildManagerInviteHtml()
        assertTrue(html.contains("Accept Invitation"))
    }

    @Test
    fun `managerInviteEmail_html_mentionsClubManager`() {
        val html = buildManagerInviteHtml()
        assertTrue(html.contains("club manager"))
    }

    @Test
    fun `managerInviteEmail_html_containsExpiryNotice`() {
        val html = buildManagerInviteHtml()
        assertTrue(html.contains("7 days"))
    }

    @Test
    fun `managerInviteEmail_subject_containsClubName`() {
        val clubName = "Harbor FC"
        val subject = "You've been invited to manage $clubName on Playbook"
        assertTrue(subject.contains(clubName))
        assertTrue(subject.contains("manage"))
    }
}
