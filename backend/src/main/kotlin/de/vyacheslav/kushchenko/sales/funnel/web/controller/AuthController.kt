package de.vyacheslav.kushchenko.sales.funnel.web.controller

import de.vyacheslav.kushchenko.sales.funnel.api.AuthApi
import de.vyacheslav.kushchenko.sales.funnel.api.model.ChangePasswordRequest
import de.vyacheslav.kushchenko.sales.funnel.api.model.RegisterRequest
import de.vyacheslav.kushchenko.sales.funnel.api.model.RegisterUserRequest
import de.vyacheslav.kushchenko.sales.funnel.api.model.SignInRequest
import de.vyacheslav.kushchenko.sales.funnel.data.user.model.toDto
import de.vyacheslav.kushchenko.sales.funnel.service.AuthenticationService
import de.vyacheslav.kushchenko.sales.funnel.util.getRequestUser
import de.vyacheslav.kushchenko.sales.funnel.util.ok
import de.vyacheslav.kushchenko.sales.funnel.web.security.annotation.Authorized
import de.vyacheslav.kushchenko.sales.funnel.web.security.annotation.IsAdmin
import org.springframework.stereotype.Component

@Component
class AuthController(private val authenticationService: AuthenticationService) : AuthApi {
    override fun register(registerRequest: RegisterRequest) = authenticationService.register(
        email = registerRequest.email, password = registerRequest.password, name = registerRequest.name
    ).toDto().ok()

    override fun signIn(signInRequest: SignInRequest) =
        authenticationService.signIn(email = signInRequest.email, password = signInRequest.password).ok()

    @Authorized
    override fun changePassword(
        changePasswordRequest: ChangePasswordRequest
    ) = authenticationService.changePassword(
        getRequestUser(),
        changePasswordRequest.oldPassword,
        changePasswordRequest.newPassword
    ).ok()

    @IsAdmin
    override fun registerUser(registerUserRequest: RegisterUserRequest) =
        authenticationService.registerUser(
            registerUserRequest.email,
            registerUserRequest.name,
        ).toDto().ok()

}
