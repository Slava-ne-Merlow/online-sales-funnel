package de.vyacheslav.kushchenko.sales.funnel.web.security

import de.vyacheslav.kushchenko.sales.funnel.service.JwtService
import de.vyacheslav.kushchenko.sales.funnel.service.UserService
import de.vyacheslav.kushchenko.sales.funnel.web.response.StatusResponse
import de.vyacheslav.kushchenko.sales.funnel.web.response.sendResponse
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class SecurityPrincipalFilter(
    private val userService: UserService,
    private val jwtService: JwtService,
    private val securityBeanPostProcessor: SecurityBeanPostProcessor
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        runCatching {
            val token = extractTokenFromRequest(request)

            if (token != null && jwtService.validateToken(token)) {
                val userId = jwtService.getUserIdFromToken(token)
                val user = userService.getById(userId)

                val authentication = UsernamePasswordAuthenticationToken(user, null, user.authorities)
                SecurityContextHolder.getContext().authentication = authentication
            }

            if (securityBeanPostProcessor.requiresAuth(request.method, request.requestURI)) {
                val authentication = SecurityContextHolder.getContext().authentication
                if (authentication == null || !authentication.isAuthenticated) {
                    response.sendResponse(StatusResponse.Error("Unauthorized", 401))
                    return
                }
            }
        }.onFailure { e ->
            e.printStackTrace()
            response.sendResponse(StatusResponse.Error("Invalid credentials", 401))
        }.onSuccess {
            filterChain.doFilter(request, response)
        }
    }

    private fun extractTokenFromRequest(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        return if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            bearerToken.substring(7)
        } else {
            null
        }
    }
}