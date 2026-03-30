package de.vyacheslav.kushchenko.sales.funnel.data.project.model

import de.vyacheslav.kushchenko.sales.funnel.api.model.ProjectEventType as ProjectEventTypeDto
import de.vyacheslav.kushchenko.sales.funnel.api.model.ProjectHistoryDto
import de.vyacheslav.kushchenko.sales.funnel.api.model.ProjectSource as ProjectSourceDto
import de.vyacheslav.kushchenko.sales.funnel.api.model.ProjectStage as ProjectStageDto
import de.vyacheslav.kushchenko.sales.funnel.api.model.ProjectStatus as ProjectStatusDto
import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectEventType
import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectSource
import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectStage
import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectStatus
import de.vyacheslav.kushchenko.sales.funnel.util.toOffsetDateTime
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class ProjectHistoryEntry(
    val id: UUID = UUID.randomUUID(),
    val projectId: UUID,
    val eventType: ProjectEventType,
    val oldStage: ProjectStage? = null,
    val newStage: ProjectStage? = null,
    val oldStatus: ProjectStatus? = null,
    val newStatus: ProjectStatus? = null,
    val oldAmount: BigDecimal? = null,
    val newAmount: BigDecimal? = null,
    val oldGlobalComment: String? = null,
    val newGlobalComment: String? = null,
    val oldSource: ProjectSource? = null,
    val newSource: ProjectSource? = null,
    val comment: String? = null,
    val actorUserId: UUID,
    val createdAt: Instant,
)

fun ProjectHistoryEntry.toDto(actorUserName: String? = null) = ProjectHistoryDto(
    id = id,
    projectId = projectId,
    eventType = ProjectEventTypeDto.valueOf(eventType.name),
    oldStage = oldStage?.let { ProjectStageDto.valueOf(it.name) },
    newStage = newStage?.let { ProjectStageDto.valueOf(it.name) },
    oldStatus = oldStatus?.let { ProjectStatusDto.valueOf(it.name) },
    newStatus = newStatus?.let { ProjectStatusDto.valueOf(it.name) },
    oldAmount = oldAmount,
    newAmount = newAmount,
    oldGlobalComment = oldGlobalComment,
    newGlobalComment = newGlobalComment,
    oldSource = oldSource?.let { ProjectSourceDto.valueOf(it.name) },
    newSource = newSource?.let { ProjectSourceDto.valueOf(it.name) },
    comment = comment,
    actorUserId = actorUserId,
    actorUserName = actorUserName,
    createdAt = createdAt.toOffsetDateTime(),
)
