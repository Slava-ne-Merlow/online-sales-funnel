package de.vyacheslav.kushchenko.sales.funnel.service

import de.vyacheslav.kushchenko.sales.funnel.api.model.AnalyticsPeriod as AnalyticsPeriodDto
import de.vyacheslav.kushchenko.sales.funnel.api.model.ProjectAnalyticsDto
import de.vyacheslav.kushchenko.sales.funnel.api.model.ProjectFunnelSummaryItemDto
import de.vyacheslav.kushchenko.sales.funnel.api.model.ProjectSource as ProjectSourceDto
import de.vyacheslav.kushchenko.sales.funnel.api.model.ProjectSourceDistributionItemDto
import de.vyacheslav.kushchenko.sales.funnel.api.model.ProjectStage as ProjectStageDto
import de.vyacheslav.kushchenko.sales.funnel.api.model.ProjectStageDistributionItemDto
import de.vyacheslav.kushchenko.sales.funnel.api.model.ProjectStatus as ProjectStatusDto
import de.vyacheslav.kushchenko.sales.funnel.api.model.ProjectStatusDistributionItemDto
import de.vyacheslav.kushchenko.sales.funnel.api.model.ProjectTrendPointDto
import de.vyacheslav.kushchenko.sales.funnel.api.model.TopProjectDto
import de.vyacheslav.kushchenko.sales.funnel.data.project.dao.ProjectEntity.Companion.asModel
import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.AnalyticsPeriod
import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectSource
import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectStage
import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectStatus
import de.vyacheslav.kushchenko.sales.funnel.data.project.model.Project
import de.vyacheslav.kushchenko.sales.funnel.data.project.model.ProjectFilter
import de.vyacheslav.kushchenko.sales.funnel.data.project.repository.ProjectRepository
import de.vyacheslav.kushchenko.sales.funnel.data.project.repository.ProjectSpecifications
import de.vyacheslav.kushchenko.sales.funnel.data.user.model.User
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class ProjectAnalyticsService(
    private val projectRepository: ProjectRepository,
    private val projectAccessService: ProjectAccessService,
    private val userService: UserService,
) {

    fun getAnalytics(
        actor: User,
        period: AnalyticsPeriod,
        source: ProjectSource?,
        responsibleUser: UUID?,
    ): ProjectAnalyticsDto {
        val now = Instant.now()
        val visibleFilter = projectAccessService.applyVisibility(
            actor,
            ProjectFilter(
                source = source,
                responsibleUser = responsibleUser,
                createdAtFrom = resolveCreatedAtFrom(period, now),
                createdAtTo = now,
            )
        )

        val projects = projectRepository.findAll(ProjectSpecifications.byFilter(visibleFilter)).map { it.asModel() }
        val totalAmount = projects.sumAmounts()
        val responsibleNames = userService.getDisplayNames(projects.mapNotNullTo(linkedSetOf()) { it.responsibleUserId })

        return ProjectAnalyticsDto(
            period = AnalyticsPeriodDto.valueOf(period.name),
            totalProjects = projects.size,
            totalAmount = totalAmount,
            inProgressAmount = projects.amountForStatus(ProjectStatus.ACTIVE),
            onHoldAmount = projects.amountForStatus(ProjectStatus.ON_HOLD),
            lostAmount = projects.amountForStatus(ProjectStatus.LOST),
            completedAmount = projects.amountForStatus(ProjectStatus.DONE),
            inactiveAmount = projects.amountForStatus(ProjectStatus.INACTIVE),
            stageDistribution = ProjectStage.entries.map { stage ->
                distributionItem(
                    count = projects.count { it.currentStage == stage },
                    amount = projects.filter { it.currentStage == stage }.sumAmounts(),
                    totalAmount = totalAmount,
                ) { count, amount, percent ->
                    ProjectStageDistributionItemDto(
                        stage = ProjectStageDto.valueOf(stage.name),
                        count = count,
                        amount = amount,
                        percentOfTotalAmount = percent,
                    )
                }
            },
            statusDistribution = ProjectStatus.entries.map { status ->
                distributionItem(
                    count = projects.count { it.currentStatus == status },
                    amount = projects.filter { it.currentStatus == status }.sumAmounts(),
                    totalAmount = totalAmount,
                ) { count, amount, percent ->
                    ProjectStatusDistributionItemDto(
                        status = ProjectStatusDto.valueOf(status.name),
                        count = count,
                        amount = amount,
                        percentOfTotalAmount = percent,
                    )
                }
            },
            sourceDistribution = ProjectSource.entries.map { projectSource ->
                distributionItem(
                    count = projects.count { it.source == projectSource },
                    amount = projects.filter { it.source == projectSource }.sumAmounts(),
                    totalAmount = totalAmount,
                ) { count, amount, percent ->
                    ProjectSourceDistributionItemDto(
                        source = ProjectSourceDto.valueOf(projectSource.name),
                        count = count,
                        amount = amount,
                        percentOfTotalAmount = percent,
                    )
                }
            },
            periodTrend = buildTrend(projects, period, now),
            funnelSummary = ProjectStage.entries.map { stage ->
                ProjectFunnelSummaryItemDto(
                    stage = ProjectStageDto.valueOf(stage.name),
                    count = projects.count { it.currentStage == stage },
                    amount = projects.filter { it.currentStage == stage }.sumAmounts(),
                )
            },
            topProjects = projects
                .sortedWith(compareByDescending<Project> { it.currentAmount ?: BigDecimal.ZERO }.thenByDescending { it.createdAt })
                .take(5)
                .map { project ->
                    TopProjectDto(
                        projectId = project.id!!,
                        title = project.title,
                        amount = project.currentAmount,
                        currentStage = ProjectStageDto.valueOf(project.currentStage.name),
                        currentStatus = ProjectStatusDto.valueOf(project.currentStatus.name),
                        responsibleUserName = project.responsibleUserId?.let(responsibleNames::get),
                    )
                },
        )
    }

    private fun buildTrend(projects: List<Project>, period: AnalyticsPeriod, now: Instant): List<ProjectTrendPointDto> {
        val zone = ZoneOffset.UTC
        return when (period) {
            AnalyticsPeriod.LAST_WEEK, AnalyticsPeriod.LAST_MONTH -> {
                val startDate = resolveCreatedAtFrom(period, now)?.atZone(zone)?.toLocalDate() ?: now.atZone(zone).toLocalDate()
                val endDate = now.atZone(zone).toLocalDate()
                val grouped = projects.groupBy { it.createdAt.atZone(zone).toLocalDate() }

                generateSequence(startDate) { current ->
                    current.plusDays(1).takeIf { !it.isAfter(endDate) }
                }.map { date ->
                    val bucketProjects = grouped[date].orEmpty()
                    ProjectTrendPointDto(
                        bucketLabel = date.toString(),
                        createdCount = bucketProjects.size,
                        createdAmount = bucketProjects.sumAmounts(),
                    )
                }.toList()
            }

            AnalyticsPeriod.LAST_YEAR, AnalyticsPeriod.ALL_TIME -> {
                val endMonth = YearMonth.from(now.atZone(zone))
                val startMonth = when (period) {
                    AnalyticsPeriod.LAST_YEAR -> YearMonth.from(now.atZone(zone).minusYears(1))
                    AnalyticsPeriod.ALL_TIME -> projects.minOfOrNull { YearMonth.from(it.createdAt.atZone(zone)) } ?: endMonth
                    else -> endMonth
                }
                val grouped = projects.groupBy { YearMonth.from(it.createdAt.atZone(zone)) }

                generateSequence(startMonth) { current ->
                    current.plusMonths(1).takeIf { !it.isAfter(endMonth) }
                }.map { month ->
                    val bucketProjects = grouped[month].orEmpty()
                    ProjectTrendPointDto(
                        bucketLabel = month.toString(),
                        createdCount = bucketProjects.size,
                        createdAmount = bucketProjects.sumAmounts(),
                    )
                }.toList()
            }
        }
    }

    private fun resolveCreatedAtFrom(period: AnalyticsPeriod, now: Instant): Instant? = when (period) {
        AnalyticsPeriod.LAST_WEEK -> now.minus(7, ChronoUnit.DAYS)
        AnalyticsPeriod.LAST_MONTH -> now.minus(30, ChronoUnit.DAYS)
        AnalyticsPeriod.LAST_YEAR -> now.minus(365, ChronoUnit.DAYS)
        AnalyticsPeriod.ALL_TIME -> null
    }

    private fun List<Project>.amountForStatus(status: ProjectStatus): BigDecimal =
        filter { it.currentStatus == status }.sumAmounts()

    private fun List<Project>.sumAmounts(): BigDecimal =
        fold(BigDecimal.ZERO) { acc, project -> acc + (project.currentAmount ?: BigDecimal.ZERO) }.setScale(2, RoundingMode.HALF_UP)

    private fun <T> distributionItem(
        count: Int,
        amount: BigDecimal,
        totalAmount: BigDecimal,
        builder: (Int, BigDecimal, BigDecimal) -> T,
    ): T = builder(count, amount, percentOfTotal(amount, totalAmount))

    private fun percentOfTotal(amount: BigDecimal, totalAmount: BigDecimal): BigDecimal {
        if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
        }

        return amount
            .multiply(BigDecimal(100))
            .divide(totalAmount, 2, RoundingMode.HALF_UP)
    }
}
