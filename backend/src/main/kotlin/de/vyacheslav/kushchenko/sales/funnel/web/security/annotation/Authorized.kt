package de.vyacheslav.kushchenko.sales.funnel.web.security.annotation

import org.springframework.security.access.prepost.PreAuthorize

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("hasRole('ADMIN') || hasRole('USER')")
annotation class Authorized
