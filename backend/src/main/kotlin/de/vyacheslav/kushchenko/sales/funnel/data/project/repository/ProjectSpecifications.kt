package de.vyacheslav.kushchenko.sales.funnel.data.project.repository

import de.vyacheslav.kushchenko.sales.funnel.data.project.dao.ProjectEntity
import de.vyacheslav.kushchenko.sales.funnel.data.project.model.ProjectFilter
import org.springframework.data.jpa.domain.Specification

object ProjectSpecifications {
    fun byFilter(filter: ProjectFilter): Specification<ProjectEntity> = Specification { root, _, cb ->
        val predicates = mutableListOf(
            filter.currentStage?.let { cb.equal(root.get<Any>("currentStage"), it) },
            filter.currentStatus?.let { cb.equal(root.get<Any>("currentStatus"), it) },
            filter.source?.let { cb.equal(root.get<Any>("source"), it) },
            filter.createdBy?.let { cb.equal(root.get<Any>("createdById"), it) },
            filter.responsibleUser?.let { cb.equal(root.get<Any>("responsibleUserId"), it) },
            filter.createdAtFrom?.let { cb.greaterThanOrEqualTo(root.get("createdAt"), it) },
            filter.createdAtTo?.let { cb.lessThanOrEqualTo(root.get("createdAt"), it) },
        ).filterNotNull()

        cb.and(*predicates.toTypedArray())
    }
}
