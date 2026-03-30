package de.vyacheslav.kushchenko.sales.funnel.data.project.repository

import de.vyacheslav.kushchenko.sales.funnel.data.project.dao.ProjectHistoryEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ProjectHistoryRepository : JpaRepository<ProjectHistoryEntity, UUID> {
    fun findAllByProjectIdOrderByCreatedAtAsc(projectId: UUID): List<ProjectHistoryEntity>
}
