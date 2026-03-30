package de.vyacheslav.kushchenko.sales.funnel.web.security

import de.vyacheslav.kushchenko.sales.funnel.data.user.model.User
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class SecurityAuthenticationProvider(
    private val userDetailsService: UserDetailsService,
    private val passwordEncoder: PasswordEncoder
) : AuthenticationProvider {
    override fun authenticate(authentication: Authentication): Authentication {
        authentication as UsernamePasswordAuthenticationToken

        val user = userDetailsService.loadUserByUsername(authentication.name) as User


        if (!passwordEncoder.matches(authentication.credentials as String, user.password)) {
            throw BadCredentialsException("")
        }

        return UsernamePasswordAuthenticationToken(user, null, user.authorities)
    }

    override fun supports(authentication: Class<*>?): Boolean {
        return authentication == UsernamePasswordAuthenticationToken::class.java
    }
}