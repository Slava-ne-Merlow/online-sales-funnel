package de.vyacheslav.kushchenko.sales.funnel.service

import de.vyacheslav.kushchenko.sales.funnel.data.mail.dao.MailOutboxEntity
import de.vyacheslav.kushchenko.sales.funnel.data.mail.repository.MailOutboxRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class MailOutboxService(
    private val repo: MailOutboxRepository,
    @Value("\${app.mail.bccAdmin:}") private val bccAdmin: String,
    @Value("\${spring.application.name}") private val applicationName: String,
    @Value("\${app.frontend-url:http://localhost:3000}") private val frontendUrl: String,
) {
    fun enqueueCredentialsEmail(toEmail: String, name: String, login: String, rawPassword: String) {
        val body = """
      Здравствуйте, $name!

      Для вас создан аккаунт в $applicationName.

      Логин: $login
      Пароль: $rawPassword
      
      Ссылка для входа: $frontendUrl
    """.trimIndent()

        repo.save(
            MailOutboxEntity(
                toEmail = toEmail,
                bccEmail = bccAdmin.ifBlank { null },
                subject = "Доступ в $applicationName",
                body = body
            )
        )
    }
}
