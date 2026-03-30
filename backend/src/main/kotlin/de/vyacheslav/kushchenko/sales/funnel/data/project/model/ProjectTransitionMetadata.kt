package de.vyacheslav.kushchenko.sales.funnel.data.project.model

import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectStage
import de.vyacheslav.kushchenko.sales.funnel.data.project.enum.ProjectStatus

data class ProjectTransitionMetadata(
    val nextStage: ProjectStage? = null,
    val canAdvanceStage: Boolean,
    val canChangeStatus: Boolean,
    val allowedStatuses: List<ProjectStatus>,
    val allowedStageTransitions: List<ProjectStage>,
)
