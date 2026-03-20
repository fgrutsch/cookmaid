package io.github.fgrutsch.cookmaid.user

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertReturning
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

interface UserRepository {
    suspend fun findByOidcSubject(oidcSubject: String): User?
    suspend fun create(oidcSubject: String): User
}

class PostgresUserRepository : UserRepository {

    override suspend fun findByOidcSubject(oidcSubject: String): User? = suspendTransaction {
        UsersTable.selectAll()
            .where(UsersTable.oidcSubject eq oidcSubject)
            .singleOrNull()
            ?.let {
                User(
                    id = it[UsersTable.id],
                    oidcSubject = it[UsersTable.oidcSubject],
                )
            }
    }

    override suspend fun create(oidcSubject: String): User = suspendTransaction {
        val row = UsersTable.insertReturning {
            it[UsersTable.oidcSubject] = oidcSubject
        }.single()

        User(
            id = row[UsersTable.id],
            oidcSubject = row[UsersTable.oidcSubject],
        )
    }
}

object UsersTable : Table("users") {
    val id = uuid("id").autoGenerate()
    val oidcSubject = text("oidc_subject").uniqueIndex()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}
