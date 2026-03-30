package de.vyacheslav.kushchenko.sales.funnel.web.exception.base

import de.vyacheslav.kushchenko.sales.funnel.web.response.WebErrorException

class NotFoundException(message: String = "Not found") : WebErrorException(message, 404)