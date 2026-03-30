package de.vyacheslav.kushchenko.sales.funnel.data.user.repository

import de.vyacheslav.kushchenko.sales.funnel.data.user.dao.UserEntity
import de.vyacheslav.kushchenko.sales.funnel.data.user.enum.UserRole
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface UserRepository : JpaRepository<UserEntity, UUID> {

    fun findAllByRoleOrderByNameAsc(role: UserRole): List<UserEntity>

    fun findByEmail(username: String): Optional<UserEntity>

    fun existsByEmail(username: String): Boolean

}
