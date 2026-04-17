package de.vyacheslav.kushchenko.sales.funnel.service

import de.vyacheslav.kushchenko.sales.funnel.api.model.CreateProjectRequest
import de.vyacheslav.kushchenko.sales.funnel.api.model.ProjectSource as ProjectSourceRequest
import de.vyacheslav.kushchenko.sales.funnel.api.model.UpdateProjectRequest
import de.vyacheslav.kushchenko.sales.funnel.data.project.dao.ProjectEntity
import de.vyacheslav.kushchenko.sales.funnel.data.project.dao.ProjectEntity.Companion.asEntity
import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectSource
import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectStage
import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectStatus
import de.vyacheslav.kushchenko.sales.funnel.data.project.model.Project
import de.vyacheslav.kushchenko.sales.funnel.data.project.repository.ProjectHistoryRepository
import de.vyacheslav.kushchenko.sales.funnel.data.project.repository.ProjectRepository
import de.vyacheslav.kushchenko.sales.funnel.data.user.enum.UserRole
import de.vyacheslav.kushchenko.sales.funnel.data.user.model.User
import de.vyacheslav.kushchenko.sales.funnel.web.exception.base.BadRequestException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.Optional
import java.util.UUID

class ProjectServiceTest {

    private val projectRepository = mockk<ProjectRepository>()
    private val projectHistoryRepository = mockk<ProjectHistoryRepository>()
    private val projectHistoryService = mockk<ProjectHistoryService>(relaxed = true)
    private val projectAccessService = ProjectAccessService()
    private val userService = mockk<UserService>()

    private lateinit var projectService: ProjectService

    @BeforeEach
    fun setUp() {
        projectService = ProjectService(
            projectRepository = projectRepository,
            projectHistoryRepository = projectHistoryRepository,
            projectHistoryService = projectHistoryService,
            projectAccessService = projectAccessService,
            userService = userService,
        )

        every { projectRepository.save(any<ProjectEntity>()) } answers { firstArg() }
    }

    @Test
    fun `user create assigns responsible user to current user automatically`() {
        val actor = testUser(UserRole.USER)

        val result = projectService.create(
            actor,
            CreateProjectRequest(
                title = "Factory lead",
                source = ProjectSourceRequest.WEBSITE,
                initialAmount = BigDecimal("1200"),
                globalComment = "Inbound request",
                responsibleUserId = null,
            ),
        )

        assertThat(result.createdById).isEqualTo(actor.id)
        assertThat(result.responsibleUserId).isEqualTo(actor.id)
        assertThat(result.initialAmount).isEqualByComparingTo("1200.00")
        assertThat(result.currentAmount).isEqualByComparingTo("1200.00")
    }

    @Test
    fun `user cannot assign another responsible user`() {
        val actor = testUser(UserRole.USER)

        assertThatThrownBy {
            projectService.create(
                actor,
                CreateProjectRequest(
                    title = "Factory lead",
                    source = ProjectSourceRequest.WEBSITE,
                    responsibleUserId = UUID.randomUUID(),
                ),
            )
        }
            .isInstanceOfSatisfying(BadRequestException::class.java) {
                assertThat(it.error.message).isEqualTo("User cannot assign another responsible user")
            }
    }

    @Test
    fun `admin create keeps responsible user null when not provided`() {
        val actor = testUser(UserRole.ADMIN)

        val result = projectService.create(
            actor,
            CreateProjectRequest(
                title = "Admin created lead",
                source = ProjectSourceRequest.TENDER,
                responsibleUserId = null,
            ),
        )

        assertThat(result.responsibleUserId).isNull()
    }

    @Test
    fun `admin can assign only regular user as responsible user`() {
        val actor = testUser(UserRole.ADMIN)
        val adminResponsible = testUser(UserRole.ADMIN)
        every { userService.getById(adminResponsible.id!!) } returns adminResponsible

        assertThatThrownBy {
            projectService.create(
                actor,
                CreateProjectRequest(
                    title = "Admin created lead",
                    source = ProjectSourceRequest.TENDER,
                    responsibleUserId = adminResponsible.id,
                ),
            )
        }
            .isInstanceOfSatisfying(BadRequestException::class.java) {
                assertThat(it.error.message).isEqualTo("Responsible user must have role USER")
            }

        verify(exactly = 1) { userService.getById(adminResponsible.id!!) }
    }

    @Test
    fun `update can change initial amount and logs it separately`() {
        val actor = testUser(UserRole.ADMIN)
        val project = testProject(createdById = actor.id!!)
        every { projectRepository.findById(project.id!!) } returns Optional.of(project.asEntity())

        val result = projectService.update(
            project.id!!,
            actor,
            UpdateProjectRequest(initialAmount = BigDecimal("1500")),
        )

        assertThat(result.initialAmount).isEqualByComparingTo("1500.00")
        assertThat(result.currentAmount).isEqualByComparingTo("1000.00")
        verify {
            projectHistoryService.logInitialAmountChanged(
                projectId = project.id!!,
                oldAmount = BigDecimal("900.00"),
                newAmount = BigDecimal("1500.00"),
                actorUserId = actor.id!!,
                createdAt = any(),
            )
        }
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
        title = "Factory lead",
        source = ProjectSource.TENDER,
        initialAmount = BigDecimal("900.00"),
        currentAmount = BigDecimal("1000.00"),
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
