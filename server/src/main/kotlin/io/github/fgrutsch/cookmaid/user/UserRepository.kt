package io.github.fgrutsch.cookmaid.user

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertReturning
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

/**
 * Persistence layer for user accounts.
 */
interface UserRepository {
    /**
     * Finds a user by their OIDC subject claim, or returns null if not registered.
     *
     * @param oidcSubject the OIDC subject claim identifying the user.
     * @return the matching user, or null if not found.
     */
    suspend fun findByOidcSubject(oidcSubject: String): User?

    /**
     * Creates a new user record for the given [oidcSubject].
     *
     * @param oidcSubject the OIDC subject claim identifying the user.
     * @return the newly created user.
     */
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
