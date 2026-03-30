package de.vyacheslav.kushchenko.sales.funnel.web.controller

import de.vyacheslav.kushchenko.sales.funnel.api.PingApi
import de.vyacheslav.kushchenko.sales.funnel.api.model.StatusResponse
import de.vyacheslav.kushchenko.sales.funnel.util.ok
import org.springframework.stereotype.Component

@Component
class PingController : PingApi {

    override fun ping() = StatusResponse("ok").ok()

}
