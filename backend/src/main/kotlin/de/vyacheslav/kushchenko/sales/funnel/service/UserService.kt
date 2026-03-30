package de.vyacheslav.kushchenko.sales.funnel.service

import de.vyacheslav.kushchenko.sales.funnel.api.model.UserUpdateRequest
import de.vyacheslav.kushchenko.sales.funnel.data.user.dao.UserEntity.Companion.asEntity
import de.vyacheslav.kushchenko.sales.funnel.data.user.dao.UserEntity.Companion.asModel
import de.vyacheslav.kushchenko.sales.funnel.data.user.enum.UserRole
import de.vyacheslav.kushchenko.sales.funnel.data.user.model.User
import de.vyacheslav.kushchenko.sales.funnel.data.user.repository.UserRepository
import de.vyacheslav.kushchenko.sales.funnel.web.exception.base.NotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UserService(
    private val userRepository: UserRepository,
) {

    fun getAll() = userRepository.findAll().map { it.asModel() }

    fun getAllRegularUsers() = userRepository.findAllByRoleOrderByNameAsc(UserRole.USER).map { it.asModel() }

    fun getDisplayNames(userIds: Set<UUID>): Map<UUID, String> {
        if (userIds.isEmpty()) {
            return emptyMap()
        }

        return userRepository.findAllById(userIds).associate { entity -> entity.id!! to entity.name }
    }

    fun getById(id: UUID): User {

        val user = userRepository.findById(id).orElseThrow { NotFoundException("User not found") }

        return user.asModel()
    }

    fun getByEmail(email: String): User {
        val user = userRepository.findByEmail(email).orElseThrow { NotFoundException("User not found") }

        return user.asModel()
    }

    fun existsByEmail(email: String) = userRepository.existsByEmail(email)

    fun update(userId: UUID, request: UserUpdateRequest): User {
        val user = getById(userId)
        val newUser = user.copy(name = request.name)
        userRepository.save(newUser.asEntity())

        return newUser
    }

    @Transactional
    fun delete(userId: UUID) {
        getById(userId)
        userRepository.deleteById(userId)
    }
}
