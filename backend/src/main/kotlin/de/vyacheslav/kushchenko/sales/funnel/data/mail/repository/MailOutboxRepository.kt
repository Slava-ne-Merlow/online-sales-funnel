package de.vyacheslav.kushchenko.sales.funnel.data.mail.repository

import de.vyacheslav.kushchenko.sales.funnel.data.mail.dao.MailOutboxEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface MailOutboxRepository : JpaRepository<MailOutboxEntity, UUID> {

    @Query("""
    select m from MailOutboxEntity m
    where m.status = 'PENDING' and m.nextAttemptAt <= :now
    order by m.createdAt asc
  """)
    fun findReady(@Param("now") now: Instant): List<MailOutboxEntity>
}
