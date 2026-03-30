package de.vyacheslav.kushchenko.sales.funnel.data.project.repository

import de.vyacheslav.kushchenko.sales.funnel.data.project.dao.ProjectEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import java.util.UUID

interface ProjectRepository : JpaRepository<ProjectEntity, UUID>, JpaSpecificationExecutor<ProjectEntity>
