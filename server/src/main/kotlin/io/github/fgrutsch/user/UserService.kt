package io.github.fgrutsch.user

import io.github.fgrutsch.auth.User
import io.github.fgrutsch.shopping.ShoppingListRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlin.uuid.Uuid

class UserService(
    private val repository: UserRepository,
    private val shoppingListRepository: ShoppingListRepository,
) {

    private val logger = KotlinLogging.logger {}
    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private val ttl = 1.minutes

    suspend fun findIdByOidcSubject(oidcSubject: String): Uuid? {
        cache[oidcSubject]?.takeIf { it.expiresAt.hasNotPassedNow() }?.let { return it.userId }

        val userId = repository.findByOidcSubject(oidcSubject)?.id ?: return null
        cache[oidcSubject] = CacheEntry(userId, TimeSource.Monotonic.markNow() + ttl)
        evictExpired()
        return userId
    }

    suspend fun getOrCreate(oidcSubject: String): User {
        val existing = repository.findByOidcSubject(oidcSubject)
        if (existing != null) {
            cache[oidcSubject] = CacheEntry(existing.id, TimeSource.Monotonic.markNow() + ttl)
            return existing
        }

        val user = suspendTransaction {
            val created = repository.create(oidcSubject)
            shoppingListRepository.createList(created.id, "Shopping List", default = true)
            created
        }
        logger.info { "New user created: id=${user.id}, oidcSubject=$oidcSubject" }
        cache[oidcSubject] = CacheEntry(user.id, TimeSource.Monotonic.markNow() + ttl)
        return user
    }

    private fun evictExpired() {
        cache.entries.removeIf { it.value.expiresAt.hasPassedNow() }
    }

    private data class CacheEntry(val userId: Uuid, val expiresAt: TimeMark)
}
