package de.vyacheslav.kushchenko.sales.funnel.service

import de.vyacheslav.kushchenko.sales.funnel.api.model.CreateProjectRequest
import de.vyacheslav.kushchenko.sales.funnel.api.model.UpdateProjectRequest
import de.vyacheslav.kushchenko.sales.funnel.data.project.dao.ProjectEntity.Companion.asEntity
import de.vyacheslav.kushchenko.sales.funnel.data.project.dao.ProjectEntity.Companion.asModel
import de.vyacheslav.kushchenko.sales.funnel.data.project.dao.ProjectHistoryEntity.Companion.asModel
import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectSource
import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectStage
import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectStatus
import de.vyacheslav.kushchenko.sales.funnel.data.project.model.Project
import de.vyacheslav.kushchenko.sales.funnel.data.project.model.ProjectFilter
import de.vyacheslav.kushchenko.sales.funnel.data.project.model.ProjectHistoryEntry
import de.vyacheslav.kushchenko.sales.funnel.data.project.repository.ProjectHistoryRepository
import de.vyacheslav.kushchenko.sales.funnel.data.project.repository.ProjectRepository
import de.vyacheslav.kushchenko.sales.funnel.data.project.repository.ProjectSpecifications
import de.vyacheslav.kushchenko.sales.funnel.data.user.enum.UserRole
import de.vyacheslav.kushchenko.sales.funnel.data.user.model.User
import de.vyacheslav.kushchenko.sales.funnel.web.exception.base.BadRequestException
import de.vyacheslav.kushchenko.sales.funnel.web.exception.base.NotFoundException
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.UUID

@Service
class ProjectService(
    private val projectRepository: ProjectRepository,
    private val projectHistoryRepository: ProjectHistoryRepository,
    private val projectHistoryService: ProjectHistoryService,
    private val projectAccessService: ProjectAccessService,
    private val userService: UserService,
) {

    @Transactional
    fun create(actor: User, createProjectRequest: CreateProjectRequest): Project {
        val now = Instant.now()
        val responsibleUserId = resolveResponsibleUserId(actor, createProjectRequest.responsibleUserId)
        val title = createProjectRequest.title.trim()

        if (title.isBlank()) {
            throw BadRequestException("Project title must not be blank")
        }

        val project = Project(
            title = title,
            source = ProjectSource.valueOf(createProjectRequest.source.name),
            initialAmount = normalizeAmount(createProjectRequest.initialAmount),
            currentAmount = normalizeAmount(createProjectRequest.initialAmount),
            globalComment = createProjectRequest.globalComment,
            currentStage = ProjectStage.QUALIFICATION,
            currentStatus = ProjectStatus.ACTIVE,
            pausedFromStage = null,
            createdById = actor.id!!,
            responsibleUserId = responsibleUserId,
            createdAt = now,
            updatedAt = now,
        )

        val savedProject = projectRepository.save(project.asEntity()).asModel()
        projectHistoryService.logCreated(savedProject, actor.id!!, now)

        return savedProject
    }

    fun getAll(actor: User, filter: ProjectFilter, sort: Sort): List<Project> {
        val visibleFilter = projectAccessService.applyVisibility(actor, filter)
        return projectRepository.findAll(ProjectSpecifications.byFilter(visibleFilter), sort).map { it.asModel() }
    }

    fun getById(projectId: UUID, actor: User): Project = projectAccessService.requireAccess(actor, getProject(projectId))

    fun getHistory(projectId: UUID, actor: User): List<ProjectHistoryEntry> {
        getById(projectId, actor)
        return projectHistoryRepository.findAllByProjectIdOrderByCreatedAtAsc(projectId).map { it.asModel() }
    }

    @Transactional
    fun update(projectId: UUID, actor: User, updateProjectRequest: UpdateProjectRequest): Project {
        val project = getById(projectId, actor)
        val now = Instant.now()

        val amountUpdated = updateProjectRequest.currentAmount != null
        val commentUpdated = updateProjectRequest.globalComment != null

        if (!amountUpdated && !commentUpdated) {
            throw BadRequestException("At least one mutable field must be provided")
        }

        val newAmount = if (amountUpdated) normalizeAmount(updateProjectRequest.currentAmount) else project.currentAmount
        val newGlobalComment = if (commentUpdated) updateProjectRequest.globalComment else project.globalComment

        val amountChanged = amountUpdated && newAmount != project.currentAmount
        val commentChanged = commentUpdated && newGlobalComment != project.globalComment

        if (!amountChanged && !commentChanged) {
            throw BadRequestException("Project update does not change any value")
        }

        val updatedProject = project.copy(
            currentAmount = newAmount,
            globalComment = newGlobalComment,
            updatedAt = now,
        )

        val savedProject = projectRepository.save(updatedProject.asEntity()).asModel()

        if (amountChanged) {
            projectHistoryService.logAmountChanged(
                projectId = savedProject.id!!,
                oldAmount = project.currentAmount,
                newAmount = savedProject.currentAmount,
                actorUserId = actor.id!!,
                createdAt = now,
            )
        }

        if (commentChanged) {
            projectHistoryService.logGlobalCommentChanged(
                projectId = savedProject.id!!,
                oldGlobalComment = project.globalComment,
                newGlobalComment = savedProject.globalComment,
                actorUserId = actor.id!!,
                createdAt = now,
            )
        }

        return savedProject
    }

    private fun getProject(projectId: UUID): Project =
        projectRepository.findById(projectId).orElseThrow { NotFoundException("Project not found") }.asModel()

    private fun resolveResponsibleUserId(actor: User, requestedResponsibleUserId: UUID?): UUID? = when (actor.role) {
        UserRole.USER -> {
            if (requestedResponsibleUserId != null && requestedResponsibleUserId != actor.id) {
                throw BadRequestException("User cannot assign another responsible user")
            }
            actor.id!!
        }

        UserRole.ADMIN -> {
            val responsibleUserId = requestedResponsibleUserId ?: return null
            val responsibleUser = userService.getById(responsibleUserId)
            if (responsibleUser.role != UserRole.USER) {
                throw BadRequestException("Responsible user must have role USER")
            }

            responsibleUser.id!!
        }
    }

    private fun normalizeAmount(amount: BigDecimal?): BigDecimal? =
        amount?.setScale(2, RoundingMode.HALF_UP)
}
