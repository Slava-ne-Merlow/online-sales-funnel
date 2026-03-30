package de.vyacheslav.kushchenko.sales.funnel.service

import de.vyacheslav.kushchenko.sales.funnel.api.model.ProjectAdvanceRequest
import de.vyacheslav.kushchenko.sales.funnel.api.model.ProjectTransitionRequest
import de.vyacheslav.kushchenko.sales.funnel.data.project.dao.ProjectEntity.Companion.asEntity
import de.vyacheslav.kushchenko.sales.funnel.data.project.dao.ProjectEntity.Companion.asModel
import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectEventType
import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectStage
import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectStatus
import de.vyacheslav.kushchenko.sales.funnel.data.project.model.Project
import de.vyacheslav.kushchenko.sales.funnel.data.project.model.ProjectTransitionMetadata
import de.vyacheslav.kushchenko.sales.funnel.data.project.repository.ProjectRepository
import de.vyacheslav.kushchenko.sales.funnel.data.user.model.User
import de.vyacheslav.kushchenko.sales.funnel.web.exception.base.BadRequestException
import de.vyacheslav.kushchenko.sales.funnel.web.exception.base.NotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class ProjectTransitionService(
    private val projectRepository: ProjectRepository,
    private val projectHistoryService: ProjectHistoryService,
    private val projectAccessService: ProjectAccessService,
) {

    private val stageTransitions = mapOf(
        ProjectStage.QUALIFICATION to setOf(ProjectStage.PROPOSAL),
        ProjectStage.PROPOSAL to setOf(ProjectStage.QUALIFICATION, ProjectStage.NEGOTIATION),
        ProjectStage.NEGOTIATION to setOf(ProjectStage.QUALIFICATION, ProjectStage.PROPOSAL, ProjectStage.INVOICE_ISSUED),
        ProjectStage.INVOICE_ISSUED to setOf(ProjectStage.NEGOTIATION, ProjectStage.PRODUCTION),
        ProjectStage.PRODUCTION to setOf(ProjectStage.INVOICE_ISSUED, ProjectStage.WAITING_FOR_PAYMENT),
        ProjectStage.WAITING_FOR_PAYMENT to emptySet(),
    )

    private val mainRouteNextStages = mapOf(
        ProjectStage.QUALIFICATION to ProjectStage.PROPOSAL,
        ProjectStage.PROPOSAL to ProjectStage.NEGOTIATION,
        ProjectStage.NEGOTIATION to ProjectStage.INVOICE_ISSUED,
        ProjectStage.INVOICE_ISSUED to ProjectStage.PRODUCTION,
        ProjectStage.PRODUCTION to ProjectStage.WAITING_FOR_PAYMENT,
        ProjectStage.WAITING_FOR_PAYMENT to null,
    )

    @Transactional
    fun transition(projectId: UUID, actor: User, projectTransitionRequest: ProjectTransitionRequest): Project {
        val project = getProject(projectId, actor)
        val comment = projectTransitionRequest.comment?.trim()?.takeIf { it.isNotEmpty() }
        val newStage = projectTransitionRequest.newStage?.let { ProjectStage.valueOf(it.name) }
        val newStatus = projectTransitionRequest.newStatus?.let { ProjectStatus.valueOf(it.name) }

        if (newStage == null && newStatus == null) {
            throw BadRequestException("At least one of newStage or newStatus must be provided")
        }

        return persistTransition(
            project = project,
            actor = actor,
            newStage = newStage,
            newStatus = newStatus,
            comment = comment,
        )
    }

    @Transactional
    fun advance(projectId: UUID, actor: User, projectAdvanceRequest: ProjectAdvanceRequest): Project {
        val project = getProject(projectId, actor)
        val nextStage = resolveNextStage(project)
            ?: throw BadRequestException("Cannot move project to the next stage from its current state")
        val comment = projectAdvanceRequest.comment?.trim()?.takeIf { it.isNotEmpty() }

        return persistTransition(
            project = project,
            actor = actor,
            newStage = nextStage,
            newStatus = null,
            comment = comment,
        )
    }

    fun getTransitionMetadata(project: Project): ProjectTransitionMetadata {
        val nextStage = resolveNextStage(project)
        val allowedStatuses = resolveAllowedStatuses(project)
        val allowedStageTransitions = resolveAllowedStageTransitions(project)

        return ProjectTransitionMetadata(
            nextStage = nextStage,
            canAdvanceStage = nextStage != null,
            canChangeStatus = allowedStatuses.isNotEmpty(),
            allowedStatuses = allowedStatuses,
            allowedStageTransitions = allowedStageTransitions,
        )
    }

    private fun persistTransition(
        project: Project,
        actor: User,
        newStage: ProjectStage?,
        newStatus: ProjectStatus?,
        comment: String?,
    ): Project {
        val stageChanged = newStage != null && newStage != project.currentStage
        val statusChanged = newStatus != null && newStatus != project.currentStatus

        if (!stageChanged && !statusChanged) {
            throw BadRequestException("Transition request does not change the project")
        }

        if (project.currentStatus == ProjectStatus.DONE) {
            throw BadRequestException("You cannot change stage or status for a completed project")
        }

        val now = Instant.now()
        val transitionResult = applyTransition(project, newStage, newStatus, comment, now)

        val savedProject = projectRepository.save(transitionResult.project.asEntity()).asModel()

        projectHistoryService.logTransition(
            oldProject = project,
            newProject = savedProject,
            eventType = transitionResult.eventType,
            comment = comment,
            actorUserId = actor.id!!,
            createdAt = now,
        )

        return savedProject
    }

    private fun getProject(projectId: UUID, actor: User): Project = projectAccessService.requireAccess(
        actor,
        projectRepository.findById(projectId)
            .orElseThrow { NotFoundException("Project not found") }
            .asModel()
    )

    private fun applyTransition(
        project: Project,
        newStage: ProjectStage?,
        newStatus: ProjectStatus?,
        comment: String?,
        now: Instant,
    ): TransitionResult {
        if (newStatus != null && newStatus != project.currentStatus) {
            validateStatusTransition(project, newStatus)
        }

        if (newStage != null && newStage != project.currentStage) {
            validateStageTransition(
                project = project,
                newStage = newStage,
                targetStatus = newStatus ?: project.currentStatus,
                comment = comment,
            )
        }

        val targetStage = newStage ?: project.currentStage
        val targetStatus = newStatus ?: project.currentStatus

        val updatedProject = project.copy(
            currentStage = targetStage,
            currentStatus = targetStatus,
            pausedFromStage = null,
            updatedAt = now,
        )

        val eventType = when {
            newStage != null && newStage != project.currentStage && newStatus != null && newStatus != project.currentStatus ->
                ProjectEventType.STAGE_STATUS_CHANGED
            newStage != null && newStage != project.currentStage -> ProjectEventType.STAGE_CHANGED
            newStatus != null && newStatus != project.currentStatus -> ProjectEventType.STATUS_CHANGED
            else -> throw BadRequestException("Transition request does not change the project")
        }

        return TransitionResult(updatedProject, eventType)
    }

    private fun validateStatusTransition(project: Project, newStatus: ProjectStatus) {
        if (project.currentStatus == ProjectStatus.DONE) {
            throw BadRequestException("You cannot change stage or status for a completed project")
        }

        if (newStatus == project.currentStatus) {
            throw BadRequestException("Transition request does not change the project")
        }
    }

    private fun validateStageTransition(
        project: Project,
        newStage: ProjectStage,
        targetStatus: ProjectStatus,
        comment: String?,
    ) {
        if (project.currentStatus != ProjectStatus.ACTIVE) {
            throw BadRequestException("Only projects in ACTIVE status can change stage")
        }

        if (targetStatus != ProjectStatus.ACTIVE) {
            throw BadRequestException("Project stage can change only while status remains ACTIVE")
        }

        val allowedTargets = stageTransitions[project.currentStage].orEmpty()
        if (newStage !in allowedTargets) {
            throw BadRequestException("Cannot move project from ${project.currentStage.name} to ${newStage.name}")
        }

        if (project.currentStage.isBackwardTo(newStage) && comment.isNullOrBlank()) {
            throw BadRequestException("Comment is required when moving project stage backwards")
        }
    }

    private fun resolveNextStage(project: Project): ProjectStage? {
        if (project.currentStatus != ProjectStatus.ACTIVE) {
            return null
        }

        return mainRouteNextStages[project.currentStage]
    }

    private fun resolveAllowedStatuses(project: Project): List<ProjectStatus> = when (project.currentStatus) {
        ProjectStatus.ACTIVE -> listOf(ProjectStatus.ON_HOLD, ProjectStatus.LOST, ProjectStatus.INACTIVE, ProjectStatus.DONE)
        ProjectStatus.ON_HOLD -> listOf(ProjectStatus.ACTIVE, ProjectStatus.LOST, ProjectStatus.INACTIVE, ProjectStatus.DONE)
        ProjectStatus.LOST -> listOf(ProjectStatus.ACTIVE, ProjectStatus.ON_HOLD, ProjectStatus.INACTIVE, ProjectStatus.DONE)
        ProjectStatus.INACTIVE -> listOf(ProjectStatus.ACTIVE, ProjectStatus.ON_HOLD, ProjectStatus.LOST, ProjectStatus.DONE)
        ProjectStatus.DONE -> emptyList()
    }

    private fun resolveAllowedStageTransitions(project: Project): List<ProjectStage> {
        if (project.currentStatus != ProjectStatus.ACTIVE) {
            return emptyList()
        }

        return stageTransitions[project.currentStage].orEmpty().toList()
    }

    private data class TransitionResult(
        val project: Project,
        val eventType: ProjectEventType,
    )
}
