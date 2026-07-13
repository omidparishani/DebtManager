package com.debtmanager.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings")

class SettingsRepository(private val context: Context) {

    companion object {
        val REMINDER_DAYS = intPreferencesKey("reminder_days")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val PIN_ENABLED = booleanPreferencesKey("pin_enabled")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val IS_UNLOCKED = booleanPreferencesKey("is_unlocked_session")
    }

    val reminderDays: Flow<Int> = context.dataStore.data.map { it[REMINDER_DAYS] ?: 3 }
    val darkMode: Flow<Boolean> = context.dataStore.data.map { it[DARK_MODE] ?: false }
    val pinEnabled: Flow<Boolean> = context.dataStore.data.map { it[PIN_ENABLED] ?: false }
    val pinHash: Flow<String?> = context.dataStore.data.map { it[PIN_HASH] }
    val biometricEnabled: Flow<Boolean> = context.dataStore.data.map { it[BIOMETRIC_ENABLED] ?: false }

    suspend fun setReminderDays(days: Int) = edit { it[REMINDER_DAYS] = days }
    suspend fun setDarkMode(enabled: Boolean) = edit { it[DARK_MODE] = enabled }
    suspend fun setPinEnabled(enabled: Boolean) = edit { it[PIN_ENABLED] = enabled }
    suspend fun setPinHash(hash: String) = edit { it[PIN_HASH] = hash }
    suspend fun setBiometricEnabled(enabled: Boolean) = edit { it[BIOMETRIC_ENABLED] = enabled }

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        context.dataStore.edit { block(it) }
    }
}
