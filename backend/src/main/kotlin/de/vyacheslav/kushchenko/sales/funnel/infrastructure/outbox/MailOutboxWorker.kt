package de.vyacheslav.kushchenko.sales.funnel.infrastructure.outbox

import de.vyacheslav.kushchenko.sales.funnel.data.mail.enum.MailOutboxStatus
import de.vyacheslav.kushchenko.sales.funnel.data.mail.repository.MailOutboxRepository
import de.vyacheslav.kushchenko.sales.funnel.infrastructure.mail.MailProvider
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.Duration

@EnableScheduling
@Component
class MailOutboxWorker(
    private val repo: MailOutboxRepository,
    private val provider: MailProvider
) {

    @Scheduled(fixedDelayString = "\${app.mail.outbox.pollMs:2000}")
    @Transactional
    fun pollAndSend() {
        val now = Instant.now()
        val items = repo.findReady(now).take(20) // лимит пачки

        for (m in items) {
            try {
                provider.sendText(m.toEmail, m.subject, m.body, m.bccEmail)
                m.status = MailOutboxStatus.SENT
                m.lastError = null
            } catch (e: Exception) {
                m.attempts += 1
                m.lastError = e.message?.take(2000)
                // простой backoff: 10s, 30s, 60s, 5m...
                val delay = when (m.attempts) {
                    1 -> Duration.ofSeconds(10)
                    2 -> Duration.ofSeconds(30)
                    3 -> Duration.ofMinutes(1)
                    else -> Duration.ofMinutes(5)
                }
                m.nextAttemptAt = now.plus(delay)

                if (m.attempts >= 10) {
                    m.status = MailOutboxStatus.FAILED
                }
            } finally {
                m.updatedAt = now
            }
        }
    }
}
