package com.playbook.email

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.simplejavamail.api.mailer.Mailer
import org.simplejavamail.email.EmailBuilder

private fun String.escapeHtml(): String = this
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
    .replace("'", "&#x27;")

// TM-044: Team approval notification
suspend fun sendTeamApprovalEmail(
    mailer: Mailer,
    toEmail: String,
    teamName: String,
    clubName: String,
    fromAddress: String,
    fromName: String,
) {
    val email = EmailBuilder.startingBlank()
        .from(fromName, fromAddress)
        .to(toEmail)
        .withSubject("Your team '$teamName' has been approved")
        .withPlainText(
            """
            Great news! Your team '$teamName' has been approved by $clubName.

            You can now manage your team and invite players via the Playbook app.
            """.trimIndent()
        )
        .withHTMLText(
            """
            <!DOCTYPE html>
            <html>
            <body style="font-family: sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
              <h2>Your team has been approved!</h2>
              <p>Your team <strong>${teamName.escapeHtml()}</strong> has been approved by <strong>${clubName.escapeHtml()}</strong>.</p>
              <p>You can now manage your team and invite players via the Playbook app.</p>
            </body>
            </html>
            """.trimIndent()
        )
        .buildEmail()

    withContext(Dispatchers.IO) { mailer.sendMail(email) }
}

// TM-045: Team rejection notification
suspend fun sendTeamRejectionEmail(
    mailer: Mailer,
    toEmail: String,
    teamName: String,
    clubName: String,
    reason: String?,
    fromAddress: String,
    fromName: String,
) {
    val reasonText = if (reason != null) "\n\nReason: $reason" else ""
    val email = EmailBuilder.startingBlank()
        .from(fromName, fromAddress)
        .to(toEmail)
        .withSubject("Update on your team '$teamName' request")
        .withPlainText(
            """
            Your request to create the team '$teamName' at $clubName was not approved.$reasonText

            Please contact your club manager for more information.
            """.trimIndent()
        )
        .withHTMLText(
            """
            <!DOCTYPE html>
            <html>
            <body style="font-family: sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
              <h2>Update on your team request</h2>
              <p>Your request to create the team <strong>${teamName.escapeHtml()}</strong> at <strong>${clubName.escapeHtml()}</strong> was not approved.</p>
              ${if (reason != null) "<p><strong>Reason:</strong> ${reason.escapeHtml()}</p>" else ""}
              <p style="color: #666;">Please contact your club manager for more information.</p>
            </body>
            </html>
            """.trimIndent()
        )
        .buildEmail()

    withContext(Dispatchers.IO) { mailer.sendMail(email) }
}
