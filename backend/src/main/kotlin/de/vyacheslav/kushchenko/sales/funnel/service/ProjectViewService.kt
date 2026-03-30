package de.vyacheslav.kushchenko.sales.funnel.service

import de.vyacheslav.kushchenko.sales.funnel.api.model.ProjectDto
import de.vyacheslav.kushchenko.sales.funnel.api.model.ProjectHistoryDto
import de.vyacheslav.kushchenko.sales.funnel.data.project.model.Project
import de.vyacheslav.kushchenko.sales.funnel.data.project.model.ProjectHistoryEntry
import de.vyacheslav.kushchenko.sales.funnel.data.project.model.toDto
import org.springframework.stereotype.Service

@Service
class ProjectViewService(
    private val userService: UserService,
    private val projectTransitionService: ProjectTransitionService,
) {

    fun toDto(project: Project): ProjectDto = toDto(listOf(project)).single()

    fun toDto(projects: List<Project>): List<ProjectDto> {
        val userNames = userService.getDisplayNames(
            projects.flatMapTo(linkedSetOf()) { project ->
                buildSet {
                    add(project.createdById)
                    project.responsibleUserId?.let(::add)
                }
            }
        )

        return projects.map { project ->
            project.toDto(
                createdByName = userNames[project.createdById],
                responsibleUserName = project.responsibleUserId?.let(userNames::get),
                metadata = projectTransitionService.getTransitionMetadata(project),
            )
        }
    }

    fun toHistoryDto(entries: List<ProjectHistoryEntry>): List<ProjectHistoryDto> {
        val userNames = userService.getDisplayNames(entries.mapTo(linkedSetOf()) { it.actorUserId })
        return entries.map { entry -> entry.toDto(actorUserName = userNames[entry.actorUserId]) }
    }
}
