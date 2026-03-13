package io.github.fgrutsch.user

import io.github.fgrutsch.auth.User
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.upsert

class PostgresUserRepository : UserRepository {

    override suspend fun getOrCreate(oidcSubject: String): User = suspendTransaction {
        val row = UsersTable.upsert(UsersTable.oidcSubject) {
            it[UsersTable.oidcSubject] = oidcSubject
        }

        User(
            id = row[UsersTable.id],
            oidcSubject = row[UsersTable.oidcSubject],
        )
    }
}

object UsersTable : Table("users") {
    val id = uuid("id").autoGenerate()
    val oidcSubject = text("oidc_subject").uniqueIndex()

    override val primaryKey = PrimaryKey(id)
}
