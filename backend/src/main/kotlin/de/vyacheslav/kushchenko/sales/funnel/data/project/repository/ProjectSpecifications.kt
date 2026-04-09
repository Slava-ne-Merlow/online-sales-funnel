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
            filter.updatedAtFrom?.let { cb.greaterThanOrEqualTo(root.get("updatedAt"), it) },
            filter.updatedAtTo?.let { cb.lessThanOrEqualTo(root.get("updatedAt"), it) },
        ).filterNotNull()

        cb.and(*predicates.toTypedArray())
    }
}
