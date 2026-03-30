package de.vyacheslav.kushchenko.sales.funnel.service

import de.vyacheslav.kushchenko.sales.funnel.data.project.model.Project
import de.vyacheslav.kushchenko.sales.funnel.data.project.model.ProjectFilter
import de.vyacheslav.kushchenko.sales.funnel.data.user.model.User
import org.springframework.stereotype.Service

@Service
class ProjectAccessService {

    fun applyVisibility(@Suppress("UNUSED_PARAMETER") actor: User, filter: ProjectFilter): ProjectFilter = filter

    fun requireAccess(@Suppress("UNUSED_PARAMETER") actor: User, project: Project): Project = project
}
