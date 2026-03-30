package de.vyacheslav.kushchenko.sales.funnel.web.exception.base

import de.vyacheslav.kushchenko.sales.funnel.web.response.WebErrorException

class ConflictException(message: String = "Conflict") : WebErrorException(message, 409)