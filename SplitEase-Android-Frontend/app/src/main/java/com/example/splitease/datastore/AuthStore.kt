package com.example.splitease.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.splitease.viewmodel.ProfileUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class AuthStore(private val context: Context) {

    private val Context.dataStore by preferencesDataStore(name = "auth")

    companion object {
        private val TOKEN = stringPreferencesKey("token")
        private val NAME = stringPreferencesKey("name")
        private val EMAIL = stringPreferencesKey("email")
        private val PHONE = stringPreferencesKey("phone")
        private val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    }

    suspend fun save(token: String, name: String, email: String, phone: String){
        context.dataStore.edit{ prefs ->
            prefs[TOKEN] = token
            prefs[NAME] = name
            prefs[EMAIL] = email
            prefs[PHONE] = phone
        }
    }

    suspend fun saveNotificationPreference(isEnabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[NOTIFICATIONS_ENABLED] = isEnabled
        }
    }

    val tokenFlow: Flow<String?> =
        context.dataStore.data.map { it[TOKEN] }

    val profileUiStateFlow: Flow<ProfileUiState> =
        context.dataStore.data.map { prefs ->
            ProfileUiState(
                name = prefs[NAME],
                email = prefs[EMAIL],
                phone = prefs[PHONE],
                isNotificationsEnabled = prefs[NOTIFICATIONS_ENABLED] ?: true
            )
        }

    suspend fun getToken(): String? =
        context.dataStore.data.first()[TOKEN]

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }


}