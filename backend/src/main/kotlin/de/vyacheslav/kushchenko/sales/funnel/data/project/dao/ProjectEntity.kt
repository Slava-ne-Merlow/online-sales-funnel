package de.vyacheslav.kushchenko.sales.funnel.data.project.dao

import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectSource
import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectStage
import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectStatus
import de.vyacheslav.kushchenko.sales.funnel.data.project.model.Project
import de.vyacheslav.kushchenko.sales.funnel.util.model.EntityConverter
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "projects")
data class ProjectEntity(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    val title: String,

    @Enumerated(EnumType.STRING)
    val source: ProjectSource,

    @Column(name = "initial_amount", precision = 19, scale = 2)
    val initialAmount: BigDecimal? = null,

    @Column(name = "current_amount", precision = 19, scale = 2)
    val currentAmount: BigDecimal? = null,

    @Column(columnDefinition = "text")
    val globalComment: String? = null,

    @Enumerated(EnumType.STRING)
    val currentStage: ProjectStage,

    @Enumerated(EnumType.STRING)
    val currentStatus: ProjectStatus,

    @Enumerated(EnumType.STRING)
    val pausedFromStage: ProjectStage? = null,

    @Column(name = "created_by")
    val createdById: UUID,

    @Column(name = "responsible_user_id")
    val responsibleUserId: UUID? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant,

    @Column(nullable = false)
    val updatedAt: Instant,
) {
    companion object : EntityConverter<Project, ProjectEntity> {
        override fun ProjectEntity.asModel() = Project(
            id = id,
            title = title,
            source = source,
            initialAmount = initialAmount,
            currentAmount = currentAmount,
            globalComment = globalComment,
            currentStage = currentStage,
            currentStatus = currentStatus,
            pausedFromStage = pausedFromStage,
            createdById = createdById,
            responsibleUserId = responsibleUserId,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

        override fun Project.asEntity() = ProjectEntity(
            id = id,
            title = title,
            source = source,
            initialAmount = initialAmount,
            currentAmount = currentAmount,
            globalComment = globalComment,
            currentStage = currentStage,
            currentStatus = currentStatus,
            pausedFromStage = pausedFromStage,
            createdById = createdById,
            responsibleUserId = responsibleUserId,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }
}
