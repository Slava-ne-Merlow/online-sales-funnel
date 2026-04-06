package de.vyacheslav.kushchenko.sales.funnel.service

import de.vyacheslav.kushchenko.sales.funnel.data.project.dao.ProjectEntity.Companion.asEntity
import de.vyacheslav.kushchenko.sales.funnel.data.project.dao.ProjectEntity.Companion.asModel
import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectStage
import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectStatus
import de.vyacheslav.kushchenko.sales.funnel.data.project.model.Project
import de.vyacheslav.kushchenko.sales.funnel.data.project.repository.ProjectRepository
import de.vyacheslav.kushchenko.sales.funnel.data.user.enum.UserRole
import de.vyacheslav.kushchenko.sales.funnel.data.user.model.User
import de.vyacheslav.kushchenko.sales.funnel.web.exception.base.BadRequestException
import de.vyacheslav.kushchenko.sales.funnel.web.request.ProjectImportRequest
import de.vyacheslav.kushchenko.sales.funnel.web.request.ProjectImportRowRequest
import de.vyacheslav.kushchenko.sales.funnel.web.response.ProjectImportResponse
import de.vyacheslav.kushchenko.sales.funnel.web.response.ProjectImportRowResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.RoundingMode

@Service
class ProjectImportService(
    private val projectRepository: ProjectRepository,
    private val projectHistoryService: ProjectHistoryService,
    private val userService: UserService,
) {

    @Transactional
    fun import(actor: User, request: ProjectImportRequest): ProjectImportResponse {
        if (request.rows.isEmpty()) {
            throw BadRequestException("Import rows must not be empty")
        }

        val rows = request.rows.map { row ->
            val normalized = normalizeRow(row)
            if (request.dryRun) {
                previewRow(normalized)
            } else {
                persistRow(actor, normalized)
            }
        }

        return ProjectImportResponse(
            dryRun = request.dryRun,
            totalRows = request.rows.size,
            processedRows = rows.count { it.action != "SKIPPED" },
            skippedRows = rows.count { it.action == "SKIPPED" },
            rows = rows,
        )
    }

    private fun previewRow(row: ProjectImportRowRequest): ProjectImportRowResponse =
        ProjectImportRowResponse(
            sheet = row.sheet,
            rowNumber = row.rowNumber,
            title = row.title,
            action = "PREVIEW",
            message = "Row is valid and ready for import",
        )

    private fun persistRow(actor: User, row: ProjectImportRowRequest): ProjectImportRowResponse {
        val responsibleUserId = resolveResponsibleUserId(actor, row.responsibleUserId)
        val createdAt = row.createdAt.toInstant()
        val project = Project(
            title = row.title.trim(),
            source = row.source,
            initialAmount = row.initialAmount.setScale(2, RoundingMode.HALF_UP),
            currentAmount = (row.currentAmount ?: row.initialAmount).setScale(2, RoundingMode.HALF_UP),
            globalComment = row.globalComment?.trim()?.takeIf { it.isNotEmpty() },
            currentStage = row.currentStage,
            currentStatus = row.currentStatus,
            pausedFromStage = null,
            createdById = actor.id!!,
            responsibleUserId = responsibleUserId,
            createdAt = createdAt,
            updatedAt = createdAt,
        )

        val savedProject = projectRepository.save(project.asEntity()).asModel()
        projectHistoryService.logCreated(savedProject, actor.id!!, createdAt)

        return ProjectImportRowResponse(
            sheet = row.sheet,
            rowNumber = row.rowNumber,
            title = row.title,
            action = "CREATED",
            projectId = savedProject.id,
        )
    }

    private fun normalizeRow(row: ProjectImportRowRequest): ProjectImportRowRequest {
        val title = row.title.trim()
        if (title.isBlank()) {
            throw BadRequestException("Import row ${row.sheet}#${row.rowNumber} has blank title")
        }

        if (row.rowNumber <= 0) {
            throw BadRequestException("Import row $title has invalid row number")
        }

        if (row.initialAmount < java.math.BigDecimal.ZERO) {
            throw BadRequestException("Import row $title has negative initialAmount")
        }

        row.currentAmount?.takeIf { it < java.math.BigDecimal.ZERO }?.let {
            throw BadRequestException("Import row $title has negative currentAmount")
        }

        when (row.currentStatus) {
            ProjectStatus.DONE -> {
                if (row.currentStage != ProjectStage.CONTRACTED) {
                    throw BadRequestException("Import row $title with DONE status must be on CONTRACTED stage")
                }
            }

            ProjectStatus.LOST, ProjectStatus.ON_HOLD -> {
                if (row.currentStage != ProjectStage.PROPOSAL) {
                    throw BadRequestException("Import row $title with ${row.currentStatus.name} status must be on PROPOSAL stage")
                }
            }

            ProjectStatus.ACTIVE, ProjectStatus.INACTIVE -> Unit
        }

        return row.copy(title = title)
    }

    private fun resolveResponsibleUserId(actor: User, requestedResponsibleUserId: java.util.UUID?): java.util.UUID? = when (actor.role) {
        UserRole.USER -> {
            if (requestedResponsibleUserId != null && requestedResponsibleUserId != actor.id) {
                throw BadRequestException("User cannot assign another responsible user during import")
            }
            actor.id!!
        }

        UserRole.ADMIN -> requestedResponsibleUserId?.let(::validateResponsibleUser)
    }

    private fun validateResponsibleUser(userId: java.util.UUID): java.util.UUID {
        val user = userService.getById(userId)
        if (user.role != UserRole.USER) {
            throw BadRequestException("Responsible user must have role USER")
        }

        return user.id!!
    }
}
