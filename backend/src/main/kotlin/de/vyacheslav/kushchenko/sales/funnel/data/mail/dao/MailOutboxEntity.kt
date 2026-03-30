package de.vyacheslav.kushchenko.sales.funnel.data.mail.dao

import de.vyacheslav.kushchenko.sales.funnel.data.mail.enum.MailOutboxStatus
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "mail_outbox")
class MailOutboxEntity(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val toEmail: String,

    @Column(nullable = true)
    val bccEmail: String? = null,

    @Column(nullable = false)
    val subject: String,

    @Column(nullable = false, columnDefinition = "text")
    val body: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: MailOutboxStatus = MailOutboxStatus.PENDING,

    @Column(nullable = false)
    var attempts: Int = 0,

    @Column(nullable = false)
    var nextAttemptAt: Instant = Instant.now(),

    @Column(nullable = true, columnDefinition = "text")
    var lastError: String? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(nullable = false)
    var updatedAt: Instant = Instant.now(),
)

