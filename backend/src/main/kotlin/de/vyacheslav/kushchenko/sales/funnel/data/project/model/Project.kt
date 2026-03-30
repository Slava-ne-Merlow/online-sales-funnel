package de.vyacheslav.kushchenko.sales.funnel.data.project.model

import de.vyacheslav.kushchenko.sales.funnel.api.model.ProjectDto
import de.vyacheslav.kushchenko.sales.funnel.api.model.ProjectSource as ProjectSourceDto
import de.vyacheslav.kushchenko.sales.funnel.api.model.ProjectStage as ProjectStageDto
import de.vyacheslav.kushchenko.sales.funnel.api.model.ProjectStatus as ProjectStatusDto
import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectSource
import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectStage
import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectStatus
import de.vyacheslav.kushchenko.sales.funnel.util.toOffsetDateTime
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class Project(
    val id: UUID? = null,
    val title: String,
    val source: ProjectSource,
    val initialAmount: BigDecimal? = null,
    val currentAmount: BigDecimal? = null,
    val globalComment: String? = null,
    val currentStage: ProjectStage,
    val currentStatus: ProjectStatus,
    val pausedFromStage: ProjectStage? = null,
    val createdById: UUID,
    val responsibleUserId: UUID? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)

fun Project.toDto(
    createdByName: String? = null,
    responsibleUserName: String? = null,
    metadata: ProjectTransitionMetadata = ProjectTransitionMetadata(
        canAdvanceStage = false,
        canChangeStatus = false,
        allowedStatuses = emptyList(),
        allowedStageTransitions = emptyList(),
    ),
) = ProjectDto(
    id = id!!,
    title = title,
    source = ProjectSourceDto.valueOf(source.name),
    initialAmount = initialAmount,
    currentAmount = currentAmount,
    globalComment = globalComment,
    currentStage = ProjectStageDto.valueOf(currentStage.name),
    currentStatus = ProjectStatusDto.valueOf(currentStatus.name),
    pausedFromStage = pausedFromStage?.let { ProjectStageDto.valueOf(it.name) },
    createdById = createdById,
    createdByName = createdByName,
    responsibleUserId = responsibleUserId,
    responsibleUserName = responsibleUserName,
    nextStage = metadata.nextStage?.let { ProjectStageDto.valueOf(it.name) },
    canAdvanceStage = metadata.canAdvanceStage,
    canChangeStatus = metadata.canChangeStatus,
    allowedStatuses = metadata.allowedStatuses.map { ProjectStatusDto.valueOf(it.name) },
    allowedStageTransitions = metadata.allowedStageTransitions.map { ProjectStageDto.valueOf(it.name) },
    createdAt = createdAt.toOffsetDateTime(),
    updatedAt = updatedAt.toOffsetDateTime(),
)
