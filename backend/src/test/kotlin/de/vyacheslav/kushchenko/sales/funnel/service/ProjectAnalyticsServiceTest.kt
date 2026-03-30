package de.vyacheslav.kushchenko.sales.funnel.service

import de.vyacheslav.kushchenko.sales.funnel.data.project.dao.ProjectEntity
import de.vyacheslav.kushchenko.sales.funnel.data.project.dao.ProjectEntity.Companion.asEntity
import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.AnalyticsPeriod
import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectSource
import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectStage
import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectStatus
import de.vyacheslav.kushchenko.sales.funnel.data.project.model.Project
import de.vyacheslav.kushchenko.sales.funnel.data.project.repository.ProjectRepository
import de.vyacheslav.kushchenko.sales.funnel.data.user.enum.UserRole
import de.vyacheslav.kushchenko.sales.funnel.data.user.model.User
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.jpa.domain.Specification
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class ProjectAnalyticsServiceTest {

    private val projectRepository = mockk<ProjectRepository>()
    private val projectAccessService = ProjectAccessService()
    private val userService = mockk<UserService>()

    private val projectAnalyticsService = ProjectAnalyticsService(
        projectRepository = projectRepository,
        projectAccessService = projectAccessService,
        userService = userService,
    )

    @Test
    fun `analytics aggregates totals distributions and top projects`() {
        val responsibleUserId = UUID.randomUUID()
        val actor = User(
            id = UUID.randomUUID(),
            email = "admin@example.com",
            name = "Admin",
            role = UserRole.ADMIN,
            password = null,
        )
        val projects = listOf(
            project(
                title = "Big contract",
                initialAmount = BigDecimal("1000.00"),
                currentAmount = BigDecimal("900.00"),
                currentStage = ProjectStage.INVOICE_ISSUED,
                currentStatus = ProjectStatus.ACTIVE,
                responsibleUserId = responsibleUserId,
            ),
            project(
                title = "Won project",
                initialAmount = BigDecimal("500.00"),
                currentAmount = BigDecimal("450.00"),
                currentStage = ProjectStage.WAITING_FOR_PAYMENT,
                currentStatus = ProjectStatus.DONE,
            ),
        )
        every { projectRepository.findAll(any<Specification<ProjectEntity>>()) } returns projects.map { it.asEntity() }
        every { userService.getDisplayNames(any()) } returns mapOf(responsibleUserId to "Alice")

        val result = projectAnalyticsService.getAnalytics(
            actor = actor,
            period = AnalyticsPeriod.ALL_TIME,
            source = null,
            responsibleUser = null,
        )

        assertThat(result.totalProjects).isEqualTo(2)
        assertThat(result.totalAmount).isEqualByComparingTo("1350.00")
        assertThat(result.inProgressAmount).isEqualByComparingTo("900.00")
        assertThat(result.lostAmount).isEqualByComparingTo("0.00")
        assertThat(result.completedAmount).isEqualByComparingTo("450.00")
        assertThat(result.stageDistribution.first { it.stage.name == ProjectStage.INVOICE_ISSUED.name }.count).isEqualTo(1)
        assertThat(result.statusDistribution.first { it.status.name == ProjectStatus.DONE.name }.amount)
            .isEqualByComparingTo("450.00")
        assertThat(result.sourceDistribution.first { it.source.name == ProjectSource.TENDER.name }.count).isEqualTo(2)
        assertThat(result.funnelSummary.first { it.stage.name == ProjectStage.WAITING_FOR_PAYMENT.name }.amount)
            .isEqualByComparingTo("450.00")
        assertThat(result.topProjects.first().title).isEqualTo("Big contract")
        assertThat(result.topProjects.first().amount).isEqualByComparingTo("900.00")
        assertThat(result.topProjects.first().responsibleUserName).isEqualTo("Alice")
        assertThat(result.periodTrend).isNotEmpty()
    }

    private fun project(
        title: String,
        initialAmount: BigDecimal,
        currentAmount: BigDecimal,
        currentStage: ProjectStage,
        currentStatus: ProjectStatus,
        responsibleUserId: UUID? = null,
    ) = Project(
        id = UUID.randomUUID(),
        title = title,
        source = ProjectSource.TENDER,
        initialAmount = initialAmount,
        currentAmount = currentAmount,
        globalComment = null,
        currentStage = currentStage,
        currentStatus = currentStatus,
        pausedFromStage = null,
        createdById = UUID.randomUUID(),
        responsibleUserId = responsibleUserId,
        createdAt = Instant.now().minus(1, ChronoUnit.DAYS),
        updatedAt = Instant.now(),
    )
}
