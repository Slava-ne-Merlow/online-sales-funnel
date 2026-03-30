package de.vyacheslav.kushchenko.sales.funnel.web.controller

import de.vyacheslav.kushchenko.sales.funnel.api.UsersApi
import de.vyacheslav.kushchenko.sales.funnel.api.model.UserDto
import de.vyacheslav.kushchenko.sales.funnel.api.model.UserUpdateRequest
import de.vyacheslav.kushchenko.sales.funnel.data.user.model.toDto
import de.vyacheslav.kushchenko.sales.funnel.service.UserService
import de.vyacheslav.kushchenko.sales.funnel.util.getRequestUser
import de.vyacheslav.kushchenko.sales.funnel.util.ok
import de.vyacheslav.kushchenko.sales.funnel.web.security.annotation.Authorized
import de.vyacheslav.kushchenko.sales.funnel.web.security.annotation.IsAdmin
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component

@Component
class UserController(
    private val userService: UserService,
) : UsersApi {
    @IsAdmin
    override fun getUsers(): ResponseEntity<List<UserDto>> = userService.getAllRegularUsers().map { it.toDto() }.ok()

    @Authorized
    override fun getMe(): ResponseEntity<UserDto> = getRequestUser().toDto().ok()

    @Authorized
    override fun updateMe(userUpdateRequest: UserUpdateRequest): ResponseEntity<UserDto> =
        userService.update(getRequestUser().id!!, userUpdateRequest).toDto().ok()
}
