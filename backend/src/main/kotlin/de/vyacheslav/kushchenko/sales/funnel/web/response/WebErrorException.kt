package de.vyacheslav.kushchenko.sales.funnel.web.response

open class WebErrorException(
    val error: StatusResponse.Error
) : RuntimeException() {
    constructor(message: String, httpStatus: Int) : this(StatusResponse.Error(message, httpStatus))
}
