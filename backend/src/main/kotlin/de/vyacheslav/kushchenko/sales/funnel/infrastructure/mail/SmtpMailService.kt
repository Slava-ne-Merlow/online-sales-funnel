package de.vyacheslav.kushchenko.sales.funnel.infrastructure.mail

import de.vyacheslav.kushchenko.sales.funnel.infrastructure.mail.MailProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(name = ["app.mail.provider"], havingValue = "smtp")
class SmtpMailService(
    private val mailSender: JavaMailSender,
    @Value("\${app.mail.from}") private val from: String
) : MailProvider {

    override fun sendText(to: String, subject: String, body: String, bcc: String?) {
        val msg = SimpleMailMessage().apply {
            from = from
            setTo(to)
            setSubject(subject)
            text = body
            if (!bcc.isNullOrBlank()) setBcc(bcc)
        }
        mailSender.send(msg)
    }
}