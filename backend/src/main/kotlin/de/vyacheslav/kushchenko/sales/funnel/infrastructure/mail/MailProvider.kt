package de.vyacheslav.kushchenko.sales.funnel.infrastructure.mail

interface MailProvider {
    fun sendText(to: String, subject: String, body: String, bcc: String? = null)
}