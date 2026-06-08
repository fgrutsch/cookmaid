package io.github.fgrutsch.cookmaid.navigation

/**
 * Constants for the web deeplink hand-off. The web entry point stashes a
 * pending deeplink in storage before authentication; [io.github.fgrutsch.cookmaid.App]
 * consumes it once the user is authenticated. Keyed via the cross-platform
 * Settings store (localStorage on web, SharedPreferences on Android).
 */
object Deeplink {
    const val KEY: String = "cookmaid.deeplink"
    const val DELETE_ACCOUNT: String = "delete-account"
}
