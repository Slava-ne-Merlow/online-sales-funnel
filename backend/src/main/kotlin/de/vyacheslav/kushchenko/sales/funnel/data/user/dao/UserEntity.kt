package de.vyacheslav.kushchenko.sales.funnel.data.user.dao

import de.vyacheslav.kushchenko.sales.funnel.data.user.enum.UserRole
import de.vyacheslav.kushchenko.sales.funnel.data.user.model.User
import de.vyacheslav.kushchenko.sales.funnel.util.model.EntityConverter
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.*

@Entity
@Table(name = "users")
data class UserEntity(

    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(name = "email", unique = true)
    val email: String,

    val name: String,

    @Enumerated(EnumType.STRING)
    val role: UserRole,

    @Column(nullable = true)
    val password: String?,

) {

    companion object : EntityConverter<User, UserEntity> {
        override fun UserEntity.asModel() = User(id, email, name, role, password)

        override fun User.asEntity() = UserEntity(id, email, name, role, password)
    }

}
