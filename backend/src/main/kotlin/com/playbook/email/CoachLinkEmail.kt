package com.playbook.email

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.simplejavamail.api.mailer.Mailer
import org.simplejavamail.email.EmailBuilder

suspend fun sendCoachLinkEmail(
    mailer: Mailer,
    toEmail: String,
    clubName: String,
    coachLink: String,
    fromAddress: String,
    fromName: String,
) {
    val email = EmailBuilder.startingBlank()
        .from(fromName, fromAddress)
        .to(toEmail)
        .withSubject("Join $clubName as a coach on Playbook")
        .withPlainText(
            """
            You've been invited to join $clubName as a coach on Playbook.

            Click the link below to get started:
            $coachLink

            This link can be used by multiple coaches and expires after a set period.

            If you didn't expect this invitation, you can safely ignore this email.
            """.trimIndent()
        )
        .withHTMLText(
            """
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
        )
        .buildEmail()

    withContext(Dispatchers.IO) { mailer.sendMail(email) }
}
