package de.vyacheslav.kushchenko.sales.funnel.service

import de.vyacheslav.kushchenko.sales.funnel.api.model.ProjectAdvanceRequest
import de.vyacheslav.kushchenko.sales.funnel.api.model.ProjectStage as ProjectStageRequest
import de.vyacheslav.kushchenko.sales.funnel.api.model.ProjectStatus as ProjectStatusRequest
import de.vyacheslav.kushchenko.sales.funnel.api.model.ProjectTransitionRequest
import de.vyacheslav.kushchenko.sales.funnel.data.project.dao.ProjectEntity
import de.vyacheslav.kushchenko.sales.funnel.data.project.dao.ProjectEntity.Companion.asEntity
import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectEventType
import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectSource
import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectStage
import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectStatus
import de.vyacheslav.kushchenko.sales.funnel.data.project.model.Project
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
import java.time.Instant
import java.util.Optional
import java.util.UUID

class ProjectTransitionServiceTest {

    private val projectRepository = mockk<ProjectRepository>()
    private val projectHistoryService = mockk<ProjectHistoryService>(relaxed = true)
    private val projectAccessService = ProjectAccessService()

    private lateinit var projectTransitionService: ProjectTransitionService

    @BeforeEach
    fun setUp() {
        projectTransitionService = ProjectTransitionService(projectRepository, projectHistoryService, projectAccessService)
        every { projectRepository.save(any<ProjectEntity>()) } answers { firstArg() }
    }

    @Test
    fun `requires comment for backward stage transition`() {
        val actor = testActor()
        val project = testProject(currentStage = ProjectStage.PROPOSAL, createdById = actor.id!!)
        every { projectRepository.findById(project.id!!) } returns Optional.of(project.asEntity())

        assertThatThrownBy {
            projectTransitionService.transition(
                project.id!!,
                actor,
                ProjectTransitionRequest(newStage = ProjectStageRequest.QUALIFICATION),
            )
        }
            .isInstanceOfSatisfying(BadRequestException::class.java) {
                assertThat(it.error.message).isEqualTo("Comment is required when moving project stage backwards")
            }
    }

    @Test
    fun `blocks stage change for non active project`() {
        val actor = testActor()
        val project = testProject(
            currentStage = ProjectStage.PROPOSAL,
            currentStatus = ProjectStatus.ON_HOLD,
            createdById = actor.id!!,
        )
        every { projectRepository.findById(project.id!!) } returns Optional.of(project.asEntity())

        assertThatThrownBy {
            projectTransitionService.transition(
                project.id!!,
                actor,
                ProjectTransitionRequest(newStage = ProjectStageRequest.NEGOTIATION),
            )
        }
            .isInstanceOfSatisfying(BadRequestException::class.java) {
                assertThat(it.error.message).isEqualTo("Only projects in ACTIVE status can change stage")
            }
    }

    @Test
    fun `blocks stage change when target status is not active`() {
        val actor = testActor()
        val project = testProject(currentStage = ProjectStage.PROPOSAL, createdById = actor.id!!)
        every { projectRepository.findById(project.id!!) } returns Optional.of(project.asEntity())

        assertThatThrownBy {
            projectTransitionService.transition(
                project.id!!,
                actor,
                ProjectTransitionRequest(
                    newStage = ProjectStageRequest.NEGOTIATION,
                    newStatus = ProjectStatusRequest.ON_HOLD,
                ),
            )
        }
            .isInstanceOfSatisfying(BadRequestException::class.java) {
                assertThat(it.error.message).isEqualTo("Project stage can change only while status remains ACTIVE")
            }
    }

    @Test
    fun `allows status change while project is on hold`() {
        val actor = testActor()
        val project = testProject(
            currentStage = ProjectStage.NEGOTIATION,
            currentStatus = ProjectStatus.ON_HOLD,
            createdById = actor.id!!,
        )
        every { projectRepository.findById(project.id!!) } returns Optional.of(project.asEntity())

        val result = projectTransitionService.transition(
            project.id!!,
            actor,
            ProjectTransitionRequest(newStatus = ProjectStatusRequest.INACTIVE),
        )

        assertThat(result.currentStatus).isEqualTo(ProjectStatus.INACTIVE)
        assertThat(result.currentStage).isEqualTo(ProjectStage.NEGOTIATION)
        assertThat(result.pausedFromStage).isNull()

        verify {
            projectHistoryService.logTransition(
                any(),
                any(),
                ProjectEventType.STATUS_CHANGED,
                null,
                actor.id!!,
                any(),
            )
        }
    }

    @Test
    fun `allows completing project with status only`() {
        val actor = testActor()
        val project = testProject(currentStage = ProjectStage.WAITING_FOR_PAYMENT, createdById = actor.id!!)
        every { projectRepository.findById(project.id!!) } returns Optional.of(project.asEntity())

        val result = projectTransitionService.transition(
            project.id!!,
            actor,
            ProjectTransitionRequest(
                newStatus = ProjectStatusRequest.DONE,
            ),
        )

        assertThat(result.currentStage).isEqualTo(ProjectStage.WAITING_FOR_PAYMENT)
        assertThat(result.currentStatus).isEqualTo(ProjectStatus.DONE)
        assertThat(result.pausedFromStage).isNull()

        verify {
            projectHistoryService.logTransition(
                any(),
                any(),
                ProjectEventType.STATUS_CHANGED,
                null,
                actor.id!!,
                any(),
            )
        }
    }

    @Test
    fun `advance moves project to next stage on main route`() {
        val actor = testActor()
        val project = testProject(currentStage = ProjectStage.PROPOSAL, createdById = actor.id!!)
        every { projectRepository.findById(project.id!!) } returns Optional.of(project.asEntity())

        val result = projectTransitionService.advance(
            project.id!!,
            actor,
            ProjectAdvanceRequest(),
        )

        assertThat(result.currentStage).isEqualTo(ProjectStage.NEGOTIATION)
        assertThat(result.currentStatus).isEqualTo(ProjectStatus.ACTIVE)
    }

    @Test
    fun `advance moves production project to waiting for payment without changing status`() {
        val actor = testActor()
        val project = testProject(currentStage = ProjectStage.PRODUCTION, createdById = actor.id!!)
        every { projectRepository.findById(project.id!!) } returns Optional.of(project.asEntity())

        val result = projectTransitionService.advance(
            project.id!!,
            actor,
            ProjectAdvanceRequest(comment = "Final payment received"),
        )

        assertThat(result.currentStage).isEqualTo(ProjectStage.WAITING_FOR_PAYMENT)
        assertThat(result.currentStatus).isEqualTo(ProjectStatus.ACTIVE)
    }

    @Test
    fun `transition metadata exposes next stage and allowed statuses for active project`() {
        val metadata = projectTransitionService.getTransitionMetadata(
            testProject(currentStage = ProjectStage.QUALIFICATION, createdById = UUID.randomUUID())
        )

        assertThat(metadata.nextStage).isEqualTo(ProjectStage.PROPOSAL)
        assertThat(metadata.canAdvanceStage).isTrue()
        assertThat(metadata.canChangeStatus).isTrue()
        assertThat(metadata.allowedStatuses).containsExactly(
            ProjectStatus.ON_HOLD,
            ProjectStatus.INACTIVE,
            ProjectStatus.DONE,
        )
        assertThat(metadata.allowedStageTransitions).containsExactly(ProjectStage.PROPOSAL)
    }

    @Test
    fun `transition metadata hides next stage for completed project`() {
        val metadata = projectTransitionService.getTransitionMetadata(
            testProject(
                currentStage = ProjectStage.QUALIFICATION,
                currentStatus = ProjectStatus.DONE,
                createdById = UUID.randomUUID(),
            )
        )

        assertThat(metadata.nextStage).isNull()
        assertThat(metadata.canAdvanceStage).isFalse()
        assertThat(metadata.allowedStatuses).isEmpty()
        assertThat(metadata.allowedStageTransitions).isEmpty()
    }

    private fun testActor() = User(
        id = UUID.randomUUID(),
        email = "actor@example.com",
        name = "Actor",
        role = UserRole.ADMIN,
        password = null,
    )

    private fun testProject(
        currentStage: ProjectStage,
        createdById: UUID,
        currentStatus: ProjectStatus = ProjectStatus.ACTIVE,
        pausedFromStage: ProjectStage? = null,
    ) = Project(
        id = UUID.randomUUID(),
        title = "Factory project",
        source = ProjectSource.TENDER,
        initialAmount = null,
        currentAmount = null,
        globalComment = null,
        currentStage = currentStage,
        currentStatus = currentStatus,
        pausedFromStage = pausedFromStage,
        createdById = createdById,
        responsibleUserId = null,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )
}
