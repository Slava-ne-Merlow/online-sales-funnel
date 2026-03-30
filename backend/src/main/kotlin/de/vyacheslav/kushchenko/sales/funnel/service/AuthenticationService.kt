package de.vyacheslav.kushchenko.sales.funnel.service

import de.vyacheslav.kushchenko.sales.funnel.api.model.AuthResponse
import de.vyacheslav.kushchenko.sales.funnel.data.auth.exception.BadPasswordException
import de.vyacheslav.kushchenko.sales.funnel.data.user.dao.UserEntity.Companion.asEntity
import de.vyacheslav.kushchenko.sales.funnel.data.user.dao.UserEntity.Companion.asModel
import de.vyacheslav.kushchenko.sales.funnel.data.user.enum.UserRole
import de.vyacheslav.kushchenko.sales.funnel.data.user.model.User
import de.vyacheslav.kushchenko.sales.funnel.data.user.model.toDto
import de.vyacheslav.kushchenko.sales.funnel.data.user.repository.UserRepository
import de.vyacheslav.kushchenko.sales.funnel.web.exception.base.ConflictException
import de.vyacheslav.kushchenko.sales.funnel.util.PasswordGenerator
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.AuthenticationException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthenticationService(
    private val authenticationManager: AuthenticationManager,
    private val userService: UserService,
    private val passwordEncoder: PasswordEncoder,
    private val userRepository: UserRepository,
    private val jwtService: JwtService,
    private val mailOutboxService: MailOutboxService,
) {

    fun register(email: String, password: String, name: String): User {
        if (userService.existsByEmail(email)) {
            throw ConflictException("User with this email already exists")
        }

        val user = User(
            email = email,
            password = passwordEncoder.encode(password),
            name = name,
            role = UserRole.ADMIN,
        )

        return userRepository.save(user.asEntity()).asModel()
    }

    fun registerUser(email: String, name: String): User {
        if (userService.existsByEmail(email)) {
            throw ConflictException("User with this email already exists")
        }

        val password = PasswordGenerator.generate(10)

        val user = User(
            email = email,
            password = passwordEncoder.encode(password),
            name = name,
            role = UserRole.USER,
        )

        mailOutboxService.enqueueCredentialsEmail(
            toEmail = email,
            name = name,
            login = email,
            rawPassword = password
        )

        val savedUser = userRepository.save(user.asEntity()).asModel()
        return savedUser
    }

    fun signIn(email: String, password: String): AuthResponse {
        try {
            authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(email, password)
            )

            val user = userService.getByEmail(email)

            val accessToken = jwtService.generateAccessToken(user.id!!)

            return AuthResponse(
                accessToken = accessToken,
                user = user.toDto(),
            )
        } catch (ex: AuthenticationException) {
            throw BadCredentialsException("", ex)
        }
    }

    fun changePassword(user: User, oldPassword: String, newPassword: String) {
        if (!passwordEncoder.matches(oldPassword, user.password)) throw BadPasswordException()

        val userEntity = user.asEntity()
            .copy(password = passwordEncoder.encode(newPassword))

        userRepository.save(userEntity)
    }
}
