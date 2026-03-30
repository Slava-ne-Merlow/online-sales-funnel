package de.vyacheslav.kushchenko.sales.funnel.web.controller

import de.vyacheslav.kushchenko.sales.funnel.api.ProjectsApi
import de.vyacheslav.kushchenko.sales.funnel.api.model.AnalyticsPeriod
import de.vyacheslav.kushchenko.sales.funnel.api.model.CreateProjectRequest
import de.vyacheslav.kushchenko.sales.funnel.api.model.ProjectAdvanceRequest
import de.vyacheslav.kushchenko.sales.funnel.api.model.ProjectAnalyticsDto
import de.vyacheslav.kushchenko.sales.funnel.api.model.ProjectDto
import de.vyacheslav.kushchenko.sales.funnel.api.model.ProjectHistoryDto
import de.vyacheslav.kushchenko.sales.funnel.api.model.ProjectSortDirection
import de.vyacheslav.kushchenko.sales.funnel.api.model.ProjectSortField
import de.vyacheslav.kushchenko.sales.funnel.api.model.ProjectSource
import de.vyacheslav.kushchenko.sales.funnel.api.model.ProjectStage
import de.vyacheslav.kushchenko.sales.funnel.api.model.ProjectStatus
import de.vyacheslav.kushchenko.sales.funnel.api.model.ProjectTransitionRequest
import de.vyacheslav.kushchenko.sales.funnel.api.model.UpdateProjectRequest
import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.AnalyticsPeriod as AnalyticsPeriodModel
import de.vyacheslav.kushchenko.sales.funnel.data.project.model.ProjectFilter
import de.vyacheslav.kushchenko.sales.funnel.service.ProjectAnalyticsService
import de.vyacheslav.kushchenko.sales.funnel.service.ProjectService
import de.vyacheslav.kushchenko.sales.funnel.service.ProjectTransitionService
import de.vyacheslav.kushchenko.sales.funnel.service.ProjectViewService
import de.vyacheslav.kushchenko.sales.funnel.util.getRequestUser
import de.vyacheslav.kushchenko.sales.funnel.util.ok
import de.vyacheslav.kushchenko.sales.funnel.web.security.annotation.Authorized
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.util.UUID

@Component
class ProjectController(
    private val projectService: ProjectService,
    private val projectTransitionService: ProjectTransitionService,
    private val projectAnalyticsService: ProjectAnalyticsService,
    private val projectViewService: ProjectViewService,
) : ProjectsApi {

    @Authorized
    override fun createProject(createProjectRequest: CreateProjectRequest): ResponseEntity<ProjectDto> =
        projectViewService.toDto(projectService.create(getRequestUser(), createProjectRequest)).ok()

    @Authorized
    override fun getProjects(
        currentStage: ProjectStage?,
        currentStatus: ProjectStatus?,
        source: ProjectSource?,
        createdBy: UUID?,
        responsibleUser: UUID?,
        createdAtFrom: OffsetDateTime?,
        createdAtTo: OffsetDateTime?,
        sortBy: ProjectSortField?,
        sortDirection: ProjectSortDirection?,
    ): ResponseEntity<List<ProjectDto>> =
        projectService.getAll(
            actor = getRequestUser(),
            filter = ProjectFilter(
                currentStage = currentStage?.let { de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectStage.valueOf(it.name) },
                currentStatus = currentStatus?.let { de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectStatus.valueOf(it.name) },
                source = source?.let { de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectSource.valueOf(it.name) },
                createdBy = createdBy,
                responsibleUser = responsibleUser,
                createdAtFrom = createdAtFrom?.toInstant(),
                createdAtTo = createdAtTo?.toInstant(),
            ),
            sort = buildSort(sortBy, sortDirection),
        ).let(projectViewService::toDto).ok()

    @Authorized
    override fun getProjectAnalytics(
        period: AnalyticsPeriod,
        source: ProjectSource?,
        responsibleUser: UUID?,
    ): ResponseEntity<ProjectAnalyticsDto> = projectAnalyticsService.getAnalytics(
        actor = getRequestUser(),
        period = AnalyticsPeriodModel.valueOf(period.name),
        source = source?.let { de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectSource.valueOf(it.name) },
        responsibleUser = responsibleUser,
    ).ok()

    @Authorized
    override fun getProject(projectId: UUID): ResponseEntity<ProjectDto> =
        projectViewService.toDto(projectService.getById(projectId, getRequestUser())).ok()

    @Authorized
    override fun updateProject(projectId: UUID, updateProjectRequest: UpdateProjectRequest): ResponseEntity<ProjectDto> =
        projectViewService.toDto(projectService.update(projectId, getRequestUser(), updateProjectRequest)).ok()

    @Authorized
    override fun transitionProject(
        projectId: UUID,
        projectTransitionRequest: ProjectTransitionRequest,
    ): ResponseEntity<ProjectDto> =
        projectViewService.toDto(projectTransitionService.transition(projectId, getRequestUser(), projectTransitionRequest)).ok()

    @Authorized
    override fun advanceProject(
        projectId: UUID,
        projectAdvanceRequest: ProjectAdvanceRequest,
    ): ResponseEntity<ProjectDto> =
        projectViewService.toDto(projectTransitionService.advance(projectId, getRequestUser(), projectAdvanceRequest)).ok()

    @Authorized
    override fun getProjectHistory(projectId: UUID): ResponseEntity<List<ProjectHistoryDto>> =
        projectViewService.toHistoryDto(projectService.getHistory(projectId, getRequestUser())).ok()

    private fun buildSort(sortBy: ProjectSortField?, sortDirection: ProjectSortDirection?): Sort {
        val property = when (sortBy ?: ProjectSortField.CREATED_AT) {
            ProjectSortField.CREATED_AT -> "createdAt"
            ProjectSortField.UPDATED_AT -> "updatedAt"
            ProjectSortField.AMOUNT -> "currentAmount"
        }
        val direction = when (sortDirection ?: ProjectSortDirection.DESC) {
            ProjectSortDirection.ASC -> Sort.Direction.ASC
            ProjectSortDirection.DESC -> Sort.Direction.DESC
        }

        return Sort.by(direction, property)
    }
}
