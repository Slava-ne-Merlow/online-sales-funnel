package de.vyacheslav.kushchenko.sales.funnel.service

import de.vyacheslav.kushchenko.sales.funnel.data.project.model.Project
import de.vyacheslav.kushchenko.sales.funnel.data.project.model.ProjectFilter
import de.vyacheslav.kushchenko.sales.funnel.data.user.enum.UserRole
import de.vyacheslav.kushchenko.sales.funnel.data.user.model.User
import de.vyacheslav.kushchenko.sales.funnel.web.exception.base.ForbiddenException
import org.springframework.stereotype.Service

@Service
class ProjectAccessService {

    fun applyVisibility(@Suppress("UNUSED_PARAMETER") actor: User, filter: ProjectFilter): ProjectFilter = filter

    fun requireAccess(@Suppress("UNUSED_PARAMETER") actor: User, project: Project): Project = project

    fun requireModificationAccess(actor: User, project: Project): Project {
        if (actor.role == UserRole.ADMIN || project.responsibleUserId == actor.id) {
            return project
        }

        throw ForbiddenException("Only project responsible user can change project")
    }
}
