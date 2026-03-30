package de.vyacheslav.kushchenko.sales.funnel.service

import de.vyacheslav.kushchenko.sales.funnel.data.user.dao.UserEntity
import de.vyacheslav.kushchenko.sales.funnel.data.user.enum.UserRole
import de.vyacheslav.kushchenko.sales.funnel.data.user.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class UserServiceTest {

    private val userRepository = mockk<UserRepository>()
    private val userService = UserService(userRepository)

    @Test
    fun `getAllRegularUsers returns only users with role USER`() {
        val regularUsers = listOf(
            userEntity(name = "Alice"),
            userEntity(name = "Bob"),
        )
        every { userRepository.findAllByRoleOrderByNameAsc(UserRole.USER) } returns regularUsers

        val result = userService.getAllRegularUsers()

        assertThat(result).hasSize(2)
        assertThat(result).extracting("role").containsOnly(UserRole.USER)
        assertThat(result).extracting("name").containsExactly("Alice", "Bob")
        verify(exactly = 1) { userRepository.findAllByRoleOrderByNameAsc(UserRole.USER) }
    }

    private fun userEntity(name: String) = UserEntity(
        id = UUID.randomUUID(),
        email = "${name.lowercase()}@example.com",
        name = name,
        role = UserRole.USER,
        password = null,
    )
}
