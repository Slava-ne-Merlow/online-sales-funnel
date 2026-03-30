package de.vyacheslav.kushchenko.sales.funnel.web.security

import de.vyacheslav.kushchenko.sales.funnel.web.response.StatusResponse
import de.vyacheslav.kushchenko.sales.funnel.web.response.sendResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.stereotype.Component
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.servlet.HandlerExceptionResolver

@Component
class SecurityHandler(
    @Qualifier("handlerExceptionResolver") private val resolver: HandlerExceptionResolver
) : AuthenticationEntryPoint {
    @Bean
    fun securityFilterChain(
        httpSecurity: HttpSecurity,
        authenticationManager: AuthenticationManager,
        securityPrincipalFilter: SecurityPrincipalFilter
    ): SecurityFilterChain {
        httpSecurity {
            csrf {
                disable()
            }

            cors {
                configurationSource = CorsConfigurationSource {
                    CorsConfiguration().applyPermitDefaultValues().apply {
                        allowedHeaders = listOf("*")
                        allowedMethods = listOf("*")
                        allowedOriginPatterns = listOf("*")
                        allowCredentials = true
                    }
                }
            }

            authorizeHttpRequests {
                authorize(anyRequest, permitAll)
            }

            exceptionHandling {
                accessDeniedHandler = AccessDeniedHandler { _, response, _ ->
                    response.sendResponse(StatusResponse.Error("Forbidden", 403))
                }

                authenticationEntryPoint = this@SecurityHandler
            }

            this.authenticationManager = authenticationManager

            addFilterBefore<UsernamePasswordAuthenticationFilter>(securityPrincipalFilter)
        }

        return httpSecurity.build()
    }

    override fun commence(
        request: HttpServletRequest, response: HttpServletResponse, authException: AuthenticationException
    ) {
        resolver.resolveException(request, response, null, authException)
    }
}
