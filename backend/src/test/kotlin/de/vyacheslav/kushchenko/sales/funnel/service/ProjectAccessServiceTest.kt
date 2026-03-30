package de.vyacheslav.kushchenko.sales.funnel.service

import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectSource
import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectStage
import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectStatus
import de.vyacheslav.kushchenko.sales.funnel.data.project.model.Project
import de.vyacheslav.kushchenko.sales.funnel.data.project.model.ProjectFilter
import de.vyacheslav.kushchenko.sales.funnel.data.user.enum.UserRole
import de.vyacheslav.kushchenko.sales.funnel.data.user.model.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class ProjectAccessServiceTest {

    private val projectAccessService = ProjectAccessService()

    @Test
    fun `project visibility keeps requested creator filter for user`() {
        val actor = testUser(UserRole.USER)
        val requestedCreator = UUID.randomUUID()
        val filter = ProjectFilter(createdBy = requestedCreator)

        val result = projectAccessService.applyVisibility(actor, filter)

        assertThat(result.createdBy).isEqualTo(requestedCreator)
    }

    @Test
    fun `user can access project created by another user`() {
        val actor = testUser(UserRole.USER)
        val project = testProject(createdById = UUID.randomUUID())

        val result = projectAccessService.requireAccess(actor, project)

        assertThat(result).isEqualTo(project)
    }

    private fun testUser(role: UserRole) = User(
        id = UUID.randomUUID(),
        email = "${role.name.lowercase()}@example.com",
        name = role.name,
        role = role,
        password = null,
    )

    private fun testProject(createdById: UUID) = Project(
        id = UUID.randomUUID(),
        title = "Project",
        source = ProjectSource.TENDER,
        initialAmount = null,
        currentAmount = null,
        globalComment = null,
        currentStage = ProjectStage.QUALIFICATION,
        currentStatus = ProjectStatus.ACTIVE,
        pausedFromStage = null,
        createdById = createdById,
        responsibleUserId = null,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )
}
