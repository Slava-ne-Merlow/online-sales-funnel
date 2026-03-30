package de.vyacheslav.kushchenko.sales.funnel.service

import de.vyacheslav.kushchenko.sales.funnel.data.project.dao.ProjectHistoryEntity.Companion.asEntity
import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectEventType
import de.vyacheslav.kushchenko.sales.funnel.data.project.model.Project
import de.vyacheslav.kushchenko.sales.funnel.data.project.model.ProjectHistoryEntry
import de.vyacheslav.kushchenko.sales.funnel.data.project.repository.ProjectHistoryRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Service
class ProjectHistoryService(
    private val projectHistoryRepository: ProjectHistoryRepository,
) {

    fun logCreated(project: Project, actorUserId: UUID, createdAt: Instant) {
        save(
            ProjectHistoryEntry(
                projectId = project.id!!,
                eventType = ProjectEventType.CREATED,
                newStage = project.currentStage,
                newStatus = project.currentStatus,
                newAmount = project.currentAmount,
                newGlobalComment = project.globalComment,
                newSource = project.source,
                actorUserId = actorUserId,
                createdAt = createdAt,
            )
        )
    }

    fun logAmountChanged(
        projectId: UUID,
        oldAmount: BigDecimal?,
        newAmount: BigDecimal?,
        actorUserId: UUID,
        createdAt: Instant,
    ) {
        save(
            ProjectHistoryEntry(
                projectId = projectId,
                eventType = ProjectEventType.AMOUNT_CHANGED,
                oldAmount = oldAmount,
                newAmount = newAmount,
                actorUserId = actorUserId,
                createdAt = createdAt,
            )
        )
    }

    fun logGlobalCommentChanged(
        projectId: UUID,
        oldGlobalComment: String?,
        newGlobalComment: String?,
        actorUserId: UUID,
        createdAt: Instant,
    ) {
        save(
            ProjectHistoryEntry(
                projectId = projectId,
                eventType = ProjectEventType.GLOBAL_COMMENT_CHANGED,
                oldGlobalComment = oldGlobalComment,
                newGlobalComment = newGlobalComment,
                actorUserId = actorUserId,
                createdAt = createdAt,
            )
        )
    }

    fun logTransition(
        oldProject: Project,
        newProject: Project,
        eventType: ProjectEventType,
        comment: String?,
        actorUserId: UUID,
        createdAt: Instant,
    ) {
        save(
            ProjectHistoryEntry(
                projectId = newProject.id!!,
                eventType = eventType,
                oldStage = oldProject.currentStage,
                newStage = newProject.currentStage,
                oldStatus = oldProject.currentStatus,
                newStatus = newProject.currentStatus,
                comment = comment,
                actorUserId = actorUserId,
                createdAt = createdAt,
            )
        )
    }

    private fun save(entry: ProjectHistoryEntry) {
        projectHistoryRepository.save(entry.asEntity())
    }
}
