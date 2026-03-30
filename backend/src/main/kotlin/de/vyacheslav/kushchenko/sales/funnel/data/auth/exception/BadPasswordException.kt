package de.vyacheslav.kushchenko.sales.funnel.data.auth.exception

import de.vyacheslav.kushchenko.sales.funnel.web.response.WebErrorException

class BadPasswordException(message: String = "Bad password") : WebErrorException(message, 400)