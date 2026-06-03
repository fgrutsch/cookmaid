package io.github.fgrutsch.cookmaid.ui.auth

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.setBody
import io.ktor.http.Parameters

/**
 * Appends the `resource` parameter to an existing form body.
 *
 * The multiplatform-oidc library builds the form body via [prepareForm]
 * before the configure block runs, so we must replace it entirely.
 */
fun HttpRequestBuilder.appendResourceToFormBody(resource: String) {
    val existing = (body as FormDataContent).formData
    setBody(FormDataContent(Parameters.build {
        existing.forEach { name, values -> values.forEach { append(name, it) } }
        append("resource", resource)
    }))
}
