package io.github.fgrutsch.cookmaid.ui.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

data class UserProfile(
    val name: String? = null,
    val email: String? = null,
    val pictureUrl: String? = null,
)

@Serializable
private data class IdTokenClaims(
    val name: String? = null,
    val email: String? = null,
    val picture: String? = null,
    @SerialName("given_name") val givenName: String? = null,
    @SerialName("family_name") val familyName: String? = null,
)

private val lenientJson = Json { ignoreUnknownKeys = true }

@OptIn(ExperimentalEncodingApi::class)
fun parseUserProfile(idToken: String?): UserProfile {
    if (idToken == null) return UserProfile()
    return try {
        val payload = idToken.split(".")[1]
        val json = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL).decode(payload).decodeToString()
        val claims = lenientJson.decodeFromString<IdTokenClaims>(json)
        UserProfile(
            name = claims.name?.ifBlank { null } ?: listOfNotNull(
                claims.givenName?.ifBlank { null },
                claims.familyName?.ifBlank { null },
            ).joinToString(" ").ifBlank { null },
            email = claims.email?.ifBlank { null },
            pictureUrl = claims.picture?.ifBlank { null },
        )
    } catch (_: Exception) {
        UserProfile()
    }
}
