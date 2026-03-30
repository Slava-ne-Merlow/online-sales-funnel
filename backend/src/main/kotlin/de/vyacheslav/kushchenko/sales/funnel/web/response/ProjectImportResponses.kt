package de.vyacheslav.kushchenko.sales.funnel.web.response

import java.util.UUID

data class ProjectImportResponse(
    val dryRun: Boolean,
    val totalRows: Int,
    val processedRows: Int,
    val skippedRows: Int,
    val rows: List<ProjectImportRowResponse>,
)

data class ProjectImportRowResponse(
    val sheet: String,
    val rowNumber: Int,
    val title: String,
    val action: String,
    val message: String? = null,
    val projectId: UUID? = null,
)
