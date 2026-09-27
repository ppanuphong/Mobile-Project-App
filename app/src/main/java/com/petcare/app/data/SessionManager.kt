package com.petcare.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.petcare.app.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.sessionStore: DataStore<Preferences> by preferencesDataStore(name = "session")

data class Session(val userId: String, val name: String, val email: String)

/**
 * เก็บ session แบบง่ายด้วย DataStore เพื่อให้เปิดแอปครั้งต่อไปไม่ต้อง login ใหม่
 * และเก็บค่าตั้งค่าการแจ้งเตือน
 */
class SessionManager(private val context: Context) {

    private object Keys {
        val USER_ID = stringPreferencesKey("user_id")
        val NAME = stringPreferencesKey("user_name")
        val EMAIL = stringPreferencesKey("user_email")
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
    }

    val session: Flow<Session?> = context.sessionStore.data.map { prefs ->
        val id = prefs[Keys.USER_ID]
        if (id.isNullOrBlank()) null
        else Session(id, prefs[Keys.NAME].orEmpty(), prefs[Keys.EMAIL].orEmpty())
    }

    val notificationsEnabled: Flow<Boolean> =
        context.sessionStore.data.map { it[Keys.NOTIFICATIONS] ?: true }

    suspend fun currentSession(): Session? = session.first()

    suspend fun isNotificationsEnabled(): Boolean = notificationsEnabled.first()

    suspend fun save(user: User) {
        context.sessionStore.edit {
            it[Keys.USER_ID] = user.id
            it[Keys.NAME] = user.name
            it[Keys.EMAIL] = user.email
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.sessionStore.edit { it[Keys.NOTIFICATIONS] = enabled }
    }

    suspend fun clear() {
        context.sessionStore.edit {
            it.remove(Keys.USER_ID)
            it.remove(Keys.NAME)
            it.remove(Keys.EMAIL)
        }
    }
}
