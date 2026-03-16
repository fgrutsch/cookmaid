package io.github.fgrutsch.cookmaid.ui.auth

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalEncodingApi::class)
class UserProfileTest {

    @Test
    fun `parses name and email from id token`() {
        val token = fakeIdToken("""{"name":"John Doe","email":"john@example.com"}""")
        val profile = parseUserProfile(token)

        assertEquals("John Doe", profile.name)
        assertEquals("john@example.com", profile.email)
    }

    @Test
    fun `parses picture url`() {
        val token = fakeIdToken("""{"name":"Jane","picture":"https://example.com/pic.jpg"}""")
        val profile = parseUserProfile(token)

        assertEquals("https://example.com/pic.jpg", profile.pictureUrl)
    }

    @Test
    fun `falls back to given and family name when name is missing`() {
        val token = fakeIdToken("""{"given_name":"John","family_name":"Doe"}""")
        val profile = parseUserProfile(token)

        assertEquals("John Doe", profile.name)
    }

    @Test
    fun `falls back to given name only when family name is missing`() {
        val token = fakeIdToken("""{"given_name":"John"}""")
        val profile = parseUserProfile(token)

        assertEquals("John", profile.name)
    }

    @Test
    fun `returns empty profile for null token`() {
        val profile = parseUserProfile(null)

        assertNull(profile.name)
        assertNull(profile.email)
        assertNull(profile.pictureUrl)
    }

    @Test
    fun `returns empty profile for malformed token`() {
        val profile = parseUserProfile("not-a-jwt")

        assertNull(profile.name)
        assertNull(profile.email)
        assertNull(profile.pictureUrl)
    }

    @Test
    fun `treats blank name as null`() {
        val token = fakeIdToken("""{"name":"  ","email":"a@b.com"}""")
        val profile = parseUserProfile(token)

        assertNull(profile.name)
    }

    @Test
    fun `treats blank email as null`() {
        val token = fakeIdToken("""{"name":"John","email":""}""")
        val profile = parseUserProfile(token)

        assertNull(profile.email)
    }

    @Test
    fun `ignores unknown claims`() {
        val token = fakeIdToken("""{"name":"John","unknown_field":"value"}""")
        val profile = parseUserProfile(token)

        assertEquals("John", profile.name)
    }

    private fun fakeIdToken(payloadJson: String): String {
        val header = Base64.UrlSafe.encode("{}".encodeToByteArray())
        val payload = Base64.UrlSafe.encode(payloadJson.encodeToByteArray())
        val signature = "sig"
        return "$header.$payload.$signature"
    }
}
