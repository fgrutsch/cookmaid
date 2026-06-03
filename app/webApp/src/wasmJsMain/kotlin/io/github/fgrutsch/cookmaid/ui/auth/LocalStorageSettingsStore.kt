package io.github.fgrutsch.cookmaid.ui.auth

import com.russhwolf.settings.StorageSettings
import org.publicvalue.multiplatform.oidc.tokenstore.SettingsStore

class LocalStorageSettingsStore : SettingsStore {
    private val settings = StorageSettings()

    override suspend fun get(key: String): String? = settings.getStringOrNull(key)

    override suspend fun put(key: String, value: String) {
        settings.putString(key, value)
    }

    override suspend fun remove(key: String) {
        settings.remove(key)
    }

    override suspend fun clear() {
        settings.clear()
    }
}
