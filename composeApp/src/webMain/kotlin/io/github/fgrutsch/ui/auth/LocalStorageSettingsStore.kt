package io.github.fgrutsch.ui.auth

import kotlinx.browser.window
import org.publicvalue.multiplatform.oidc.tokenstore.SettingsStore

class LocalStorageSettingsStore : SettingsStore {
    private val storage = window.localStorage

    override suspend fun get(key: String): String? = storage.getItem(key)

    override suspend fun put(key: String, value: String) {
        storage.setItem(key, value)
    }

    override suspend fun remove(key: String) {
        storage.removeItem(key)
    }

    override suspend fun clear() {
        storage.clear()
    }
}
