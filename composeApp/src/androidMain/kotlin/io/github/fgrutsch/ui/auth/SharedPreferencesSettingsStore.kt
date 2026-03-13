package io.github.fgrutsch.ui.auth

import android.content.Context
import org.publicvalue.multiplatform.oidc.tokenstore.SettingsStore

class SharedPreferencesSettingsStore(context: Context) : SettingsStore {
    private val prefs = context.getSharedPreferences("oidc_tokens", Context.MODE_PRIVATE)

    override suspend fun get(key: String): String? = prefs.getString(key, null)

    override suspend fun put(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override suspend fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    override suspend fun clear() {
        prefs.edit().clear().apply()
    }
}
