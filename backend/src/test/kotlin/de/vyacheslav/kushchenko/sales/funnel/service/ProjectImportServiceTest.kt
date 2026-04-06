package de.vyacheslav.kushchenko.sales.funnel.service

import de.vyacheslav.kushchenko.sales.funnel.data.project.dao.ProjectEntity
import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectSource
import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectStage
import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectStatus
import de.vyacheslav.kushchenko.sales.funnel.data.project.repository.ProjectRepository
import de.vyacheslav.kushchenko.sales.funnel.data.user.enum.UserRole
import de.vyacheslav.kushchenko.sales.funnel.data.user.model.User
import de.vyacheslav.kushchenko.sales.funnel.web.exception.base.BadRequestException
import de.vyacheslav.kushchenko.sales.funnel.web.request.ProjectImportRequest
import de.vyacheslav.kushchenko.sales.funnel.web.request.ProjectImportRowRequest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class ProjectImportServiceTest {

    private val projectRepository = mockk<ProjectRepository>()
    private val projectHistoryService = mockk<ProjectHistoryService>(relaxed = true)
    private val userService = mockk<UserService>(relaxed = true)

    private lateinit var projectImportService: ProjectImportService

    @BeforeEach
    fun setUp() {
        projectImportService = ProjectImportService(projectRepository, projectHistoryService, userService)
        every { projectRepository.save(any<ProjectEntity>()) } answers { firstArg() }
    }

    @Test
    fun `dry run validates rows without saving`() {
        val actor = admin()
        val response = projectImportService.import(
            actor,
            ProjectImportRequest(
                dryRun = true,
                rows = listOf(validRow()),
            ),
        )

        assertThat(response.dryRun).isTrue()
        assertThat(response.processedRows).isEqualTo(1)
        assertThat(response.rows.single().action).isEqualTo("PREVIEW")
        verify(exactly = 0) { projectRepository.save(any<ProjectEntity>()) }
    }

    @Test
    fun `apply persists project with imported state`() {
        val actor = admin()
        val createdAt = OffsetDateTime.of(2026, 3, 27, 0, 0, 0, 0, ZoneOffset.UTC)

        val response = projectImportService.import(
            actor,
            ProjectImportRequest(
                dryRun = false,
                rows = listOf(validRow(createdAt = createdAt, currentStage = ProjectStage.PROPOSAL, currentStatus = ProjectStatus.LOST)),
            ),
        )

        assertThat(response.rows.single().action).isEqualTo("CREATED")

        verify {
            projectRepository.save(withArg { entity ->
                assertThat(entity.title).isEqualTo("Проект")
                assertThat(entity.source).isEqualTo(ProjectSource.DIRECT_SALES)
                assertThat(entity.initialAmount).isEqualByComparingTo("1200.00")
                assertThat(entity.currentAmount).isEqualByComparingTo("1200.00")
                assertThat(entity.currentStage).isEqualTo(ProjectStage.PROPOSAL)
                assertThat(entity.currentStatus).isEqualTo(ProjectStatus.LOST)
                assertThat(entity.createdAt).isEqualTo(createdAt.toInstant())
                assertThat(entity.updatedAt).isEqualTo(createdAt.toInstant())
            })
        }
    }

    @Test
    fun `done rows must use contracted stage`() {
        val actor = admin()

        assertThatThrownBy {
            projectImportService.import(
                actor,
                ProjectImportRequest(
                    dryRun = true,
                    rows = listOf(validRow(currentStage = ProjectStage.INVOICE_ISSUED, currentStatus = ProjectStatus.DONE)),
                ),
            )
        }
            .isInstanceOfSatisfying(BadRequestException::class.java) {
                assertThat(it.error.message).isEqualTo("Import row Проект with DONE status must be on CONTRACTED stage")
            }
    }

    @Test
    fun `user import assigns responsible user to actor`() {
        val actor = User(
            id = UUID.randomUUID(),
            email = "user@example.com",
            name = "User",
            role = UserRole.USER,
            password = null,
        )

        projectImportService.import(
            actor,
            ProjectImportRequest(
                dryRun = false,
                rows = listOf(validRow()),
            ),
        )

        verify {
            projectRepository.save(withArg { entity ->
                assertThat(entity.createdById).isEqualTo(actor.id)
                assertThat(entity.responsibleUserId).isEqualTo(actor.id)
            })
        }
    }

    private fun validRow(
        createdAt: OffsetDateTime = OffsetDateTime.of(2026, 3, 20, 0, 0, 0, 0, ZoneOffset.UTC),
        currentStage: ProjectStage = ProjectStage.PROPOSAL,
        currentStatus: ProjectStatus = ProjectStatus.ACTIVE,
    ) = ProjectImportRowRequest(
        sheet = "Певцов",
        rowNumber = 101,
        title = "Проект",
        source = ProjectSource.DIRECT_SALES,
        initialAmount = BigDecimal("1200"),
        currentAmount = null,
        globalComment = "Комментарий",
        currentStage = currentStage,
        currentStatus = currentStatus,
        createdAt = createdAt,
        responsibleUserId = null,
    )

    private fun admin() = User(
        id = UUID.randomUUID(),
        email = "admin@example.com",
        name = "Admin",
        role = UserRole.ADMIN,
        password = null,
    )
}
