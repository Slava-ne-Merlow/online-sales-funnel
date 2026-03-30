package de.vyacheslav.kushchenko.sales.funnel.data.project.dao

import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectEventType
import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectSource
import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectStage
import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectStatus
import de.vyacheslav.kushchenko.sales.funnel.data.project.model.ProjectHistoryEntry
import de.vyacheslav.kushchenko.sales.funnel.util.model.EntityConverter
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "project_history")
data class ProjectHistoryEntity(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "project_id")
    val projectId: UUID,

    @Enumerated(EnumType.STRING)
    val eventType: ProjectEventType,

    @Enumerated(EnumType.STRING)
    val oldStage: ProjectStage? = null,

    @Enumerated(EnumType.STRING)
    val newStage: ProjectStage? = null,

    @Enumerated(EnumType.STRING)
    val oldStatus: ProjectStatus? = null,

    @Enumerated(EnumType.STRING)
    val newStatus: ProjectStatus? = null,

    @Column(precision = 19, scale = 2)
    val oldAmount: BigDecimal? = null,

    @Column(precision = 19, scale = 2)
    val newAmount: BigDecimal? = null,

    @Column(columnDefinition = "text")
    val oldGlobalComment: String? = null,

    @Column(columnDefinition = "text")
    val newGlobalComment: String? = null,

    @Enumerated(EnumType.STRING)
    val oldSource: ProjectSource? = null,

    @Enumerated(EnumType.STRING)
    val newSource: ProjectSource? = null,

    @Column(columnDefinition = "text")
    val comment: String? = null,

    @Column(name = "actor_user_id")
    val actorUserId: UUID,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant,
) {
    companion object : EntityConverter<ProjectHistoryEntry, ProjectHistoryEntity> {
        override fun ProjectHistoryEntity.asModel() = ProjectHistoryEntry(
            id = id,
            projectId = projectId,
            eventType = eventType,
            oldStage = oldStage,
            newStage = newStage,
            oldStatus = oldStatus,
            newStatus = newStatus,
            oldAmount = oldAmount,
            newAmount = newAmount,
            oldGlobalComment = oldGlobalComment,
            newGlobalComment = newGlobalComment,
            oldSource = oldSource,
            newSource = newSource,
            comment = comment,
            actorUserId = actorUserId,
            createdAt = createdAt,
        )

        override fun ProjectHistoryEntry.asEntity() = ProjectHistoryEntity(
            id = id,
            projectId = projectId,
            eventType = eventType,
            oldStage = oldStage,
            newStage = newStage,
            oldStatus = oldStatus,
            newStatus = newStatus,
            oldAmount = oldAmount,
            newAmount = newAmount,
            oldGlobalComment = oldGlobalComment,
            newGlobalComment = newGlobalComment,
            oldSource = oldSource,
            newSource = newSource,
            comment = comment,
            actorUserId = actorUserId,
            createdAt = createdAt,
        )
    }
}
