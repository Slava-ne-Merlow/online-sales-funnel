package de.vyacheslav.kushchenko.sales.funnel.web.exception.base

import de.vyacheslav.kushchenko.sales.funnel.web.response.WebErrorException

class ForbiddenException(message: String = "Forbidden") : WebErrorException(message, 403)
