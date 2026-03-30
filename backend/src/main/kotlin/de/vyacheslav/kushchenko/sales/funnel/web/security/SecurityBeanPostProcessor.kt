package de.vyacheslav.kushchenko.sales.funnel.web.security

import de.vyacheslav.kushchenko.sales.funnel.web.security.annotation.Authorized
import de.vyacheslav.kushchenko.sales.funnel.web.security.annotation.IsAdmin
import jakarta.annotation.PostConstruct
import org.springframework.http.server.PathContainer
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import org.springframework.web.util.pattern.PathPattern
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.kotlinFunction

@Component
class SecurityBeanPostProcessor(
    private val requestMappingHandlerMapping: RequestMappingHandlerMapping
) {
    private val authorizedEndpoints = mutableListOf<Pair<PathPattern, String>>()

    @PostConstruct
    fun scan() {
        val handlerMethods = requestMappingHandlerMapping.handlerMethods
        for ((info, handlerMethod) in handlerMethods) {
            val method = handlerMethod.method.kotlinFunction
            val hasAnnotation =
                method?.findAnnotation<Authorized>() != null || method?.findAnnotation<IsAdmin>() != null
            if (hasAnnotation) {
                val pathPatternsCondition = info.pathPatternsCondition ?: continue
                for (pattern in pathPatternsCondition.patterns) {
                    for (requestMethod in info.methodsCondition.methods) {
                        authorizedEndpoints += pattern to requestMethod.name
                    }
                }
            }
        }
    }

    fun requiresAuth(method: String, path: String): Boolean {
        return authorizedEndpoints.any { it.first.matches(PathContainer.parsePath(path)) && it.second == method }
    }
}
