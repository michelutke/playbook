package com.playbook.email

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.simplejavamail.api.mailer.Mailer
import org.simplejavamail.email.EmailBuilder

suspend fun sendManagerInviteEmail(
    mailer: Mailer,
    toEmail: String,
    clubName: String,
    inviteLink: String,
    fromAddress: String,
    fromName: String,
) {
    val email = EmailBuilder.startingBlank()
        .from(fromName, fromAddress)
        .to(toEmail)
        .withSubject("You've been invited to manage $clubName on Playbook")
        .withPlainText(
            """
            You've been invited to become a club manager for $clubName on Playbook.

            Click the link below to accept your invitation:
            $inviteLink

            This invite expires in 7 days.

            If you didn't expect this invitation, you can safely ignore this email.
            """.trimIndent()
        )
        .withHTMLText(
            """
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
        )
        .buildEmail()

    withContext(Dispatchers.IO) { mailer.sendMail(email) }
}
