package io.github.fgrutsch.cookmaid.navigation

/**
 * Identifiers for web deeplinks. The web entry point maps the initial
 * `window.location` path to one of these and passes it into
 * [io.github.fgrutsch.cookmaid.App] as `startDeeplink`; [io.github.fgrutsch.cookmaid.App]
 * navigates to the matching route once the user is authenticated.
 *
 * No persistence is needed: the wasmJS OIDC login flow is a popup, so the main
 * window stays on the deeplink URL throughout login and the in-memory value
 * survives.
 */
object Deeplink {
    const val DELETE_ACCOUNT: String = "delete-account"
}
