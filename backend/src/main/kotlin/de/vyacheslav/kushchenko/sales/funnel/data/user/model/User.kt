package de.vyacheslav.kushchenko.sales.funnel.data.user.model

import de.vyacheslav.kushchenko.sales.funnel.api.model.UserDto
import de.vyacheslav.kushchenko.sales.funnel.api.model.Role as RoleDto
import de.vyacheslav.kushchenko.sales.funnel.data.user.enum.UserRole
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.*

data class User(
    val id: UUID? = null,
    val email: String,
    val name: String,
    val role: UserRole,
    @get:JvmName("getPassword0")
    val password: String?,
) : UserDetails {

    override fun getAuthorities() = mutableSetOf(SimpleGrantedAuthority("ROLE_${role.name}"))

    override fun getPassword(): String? = password

    override fun getUsername(): String = email

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true

}

fun User.toDto() = UserDto(
    id = this.id!!,
    name = this.name,
    email = this.email,
    role = RoleDto.valueOf(this.role.name)
)
