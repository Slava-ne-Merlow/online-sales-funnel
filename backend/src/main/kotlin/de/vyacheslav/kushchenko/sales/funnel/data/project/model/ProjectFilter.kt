package de.vyacheslav.kushchenko.sales.funnel.data.project.model

import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectSource
import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectStage
import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectStatus
import java.time.Instant
import java.util.UUID

data class ProjectFilter(
    val currentStage: ProjectStage? = null,
    val currentStatus: ProjectStatus? = null,
    val source: ProjectSource? = null,
    val createdBy: UUID? = null,
    val responsibleUser: UUID? = null,
    val updatedAtFrom: Instant? = null,
    val updatedAtTo: Instant? = null,
)
