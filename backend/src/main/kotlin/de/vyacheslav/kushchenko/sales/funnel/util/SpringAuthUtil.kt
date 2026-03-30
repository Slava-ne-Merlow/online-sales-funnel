package de.vyacheslav.kushchenko.sales.funnel.util

import de.vyacheslav.kushchenko.sales.funnel.data.user.model.User
import org.springframework.security.core.context.SecurityContextHolder

fun getRequestUser(): User {
    val authentication = SecurityContextHolder.getContext().authentication
    return authentication.principal as User
}
