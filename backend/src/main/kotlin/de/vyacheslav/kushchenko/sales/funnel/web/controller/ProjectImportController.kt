package de.vyacheslav.kushchenko.sales.funnel.web.controller

import de.vyacheslav.kushchenko.sales.funnel.service.ProjectImportService
import de.vyacheslav.kushchenko.sales.funnel.util.getRequestUser
import de.vyacheslav.kushchenko.sales.funnel.util.ok
import de.vyacheslav.kushchenko.sales.funnel.web.request.ProjectImportRequest
import de.vyacheslav.kushchenko.sales.funnel.web.response.ProjectImportResponse
import de.vyacheslav.kushchenko.sales.funnel.web.security.annotation.Authorized
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/internal/project-import")
class ProjectImportController(
    private val projectImportService: ProjectImportService,
) {

    @Authorized
    @PostMapping("/rows")
    fun importRows(@RequestBody request: ProjectImportRequest): org.springframework.http.ResponseEntity<ProjectImportResponse> =
        projectImportService.import(getRequestUser(), request).ok()
}
