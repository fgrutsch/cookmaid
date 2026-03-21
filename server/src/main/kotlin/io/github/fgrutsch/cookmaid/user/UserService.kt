package io.github.fgrutsch.cookmaid.user

import io.github.fgrutsch.cookmaid.shopping.ShoppingListRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Manages user lookup and auto-provisioning with a short-lived in-memory cache.
 */
class UserService(
    private val repository: UserRepository,
    private val shoppingListRepository: ShoppingListRepository,
) {

    private val logger = KotlinLogging.logger {}
    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private val ttl = 1.minutes

    /**
     * Returns the cached or persisted [UserId] for the given [oidcSubject], or null if unknown.
     *
     * @param oidcSubject the OIDC subject claim identifying the user.
     * @return the user's id, or null if no matching user exists.
     */
    suspend fun findIdByOidcSubject(oidcSubject: String): UserId? {
        cache[oidcSubject]?.takeIf { it.expiresAt.hasNotPassedNow() }?.let { return it.userId }

        val userId = repository.findByOidcSubject(oidcSubject)?.let { UserId(it.id) }
        if (userId != null) {
            cache[oidcSubject] = CacheEntry(userId, TimeSource.Monotonic.markNow() + ttl)
            evictExpired()
        }
        return userId
    }

    /**
     * Returns the existing user or creates a new one (with a default shopping list)
     * for the given [oidcSubject].
     *
     * @param oidcSubject the OIDC subject claim identifying the user.
     * @return the existing or newly created user.
     */
    suspend fun getOrCreate(oidcSubject: String): User {
        val existing = repository.findByOidcSubject(oidcSubject)
        if (existing != null) {
            cache[oidcSubject] = CacheEntry(UserId(existing.id), TimeSource.Monotonic.markNow() + ttl)
            return existing
        }

        val user = suspendTransaction {
            val created = repository.create(oidcSubject)
            shoppingListRepository.createList(UserId(created.id), "Shopping List", default = true)
            created
        }
        logger.info { "New user created: id=${user.id}, oidcSubject=$oidcSubject" }
        cache[oidcSubject] = CacheEntry(UserId(user.id), TimeSource.Monotonic.markNow() + ttl)
        return user
    }

    private fun evictExpired() {
        cache.entries.removeIf { it.value.expiresAt.hasPassedNow() }
    }

    private data class CacheEntry(val userId: UserId, val expiresAt: TimeMark)
}
