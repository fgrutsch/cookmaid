package io.github.fgrutsch.cookmaid.ui.auth

import io.ktor.http.URLBuilder

/**
 * Appends the login-specific parameters to the OIDC authorization URL.
 *
 * `prompt=consent` is only sent with `offline_access`: providers ignore `offline_access`
 * without it, while sending it unconditionally can show a consent screen on every login.
 *
 * @param scope the space-separated scopes requested at login.
 * @param resource the API resource indicator, or null when none is configured.
 */
fun URLBuilder.appendLoginParameters(scope: String, resource: String?) {
    if ("offline_access" in scope.split(" ")) {
        parameters.append("prompt", "consent")
    }
    if (resource != null) {
        parameters.append("resource", resource)
    }
}
