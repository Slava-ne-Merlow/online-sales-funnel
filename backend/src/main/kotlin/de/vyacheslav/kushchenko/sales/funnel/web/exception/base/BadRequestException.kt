package de.vyacheslav.kushchenko.sales.funnel.web.exception.base

import de.vyacheslav.kushchenko.sales.funnel.web.response.WebErrorException

class BadRequestException(message: String = "Bad request") : WebErrorException(message, 400)
