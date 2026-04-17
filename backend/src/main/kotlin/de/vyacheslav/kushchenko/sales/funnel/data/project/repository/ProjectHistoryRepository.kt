package de.vyacheslav.kushchenko.sales.funnel.data.project.repository

import de.vyacheslav.kushchenko.sales.funnel.data.project.dao.ProjectHistoryEntity
import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectEventType
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

interface ProjectHistoryRepository : JpaRepository<ProjectHistoryEntity, UUID> {
    fun findAllByProjectIdOrderByCreatedAtAsc(projectId: UUID): List<ProjectHistoryEntity>
    fun findAllByEventTypeInAndCreatedAtBetween(
        eventTypes: Collection<ProjectEventType>,
        createdAtFrom: Instant,
        createdAtTo: Instant,
    ): List<ProjectHistoryEntity>
}
