package de.vyacheslav.kushchenko.sales.funnel.web.request

import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectSource
import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectStage
import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectStatus
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class ProjectImportRequest(
    val dryRun: Boolean = true,
    val rows: List<ProjectImportRowRequest> = emptyList(),
)

data class ProjectImportRowRequest(
    val sheet: String,
    val rowNumber: Int,
    val title: String,
    val source: ProjectSource,
    val initialAmount: BigDecimal,
    val currentAmount: BigDecimal? = null,
    val globalComment: String? = null,
    val currentStage: ProjectStage,
    val currentStatus: ProjectStatus,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime? = null,
    val responsibleUserId: UUID? = null,
)
