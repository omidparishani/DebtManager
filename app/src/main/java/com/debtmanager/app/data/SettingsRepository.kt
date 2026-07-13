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
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val NOTIFICATION_SOUND = stringPreferencesKey("notification_sound")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val REMIND_ON_DUE_DAY = booleanPreferencesKey("remind_on_due_day")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val PIN_ENABLED = booleanPreferencesKey("pin_enabled")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val IS_UNLOCKED = booleanPreferencesKey("is_unlocked_session")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_ICON = stringPreferencesKey("user_icon")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    }

    val reminderDays: Flow<Int> = context.dataStore.data.map { it[REMINDER_DAYS] ?: 3 }
    val reminderHour: Flow<Int> = context.dataStore.data.map { it[REMINDER_HOUR] ?: 9 }
    val notificationSound: Flow<String> = context.dataStore.data.map { it[NOTIFICATION_SOUND] ?: "default" }
    val vibrationEnabled: Flow<Boolean> = context.dataStore.data.map { it[VIBRATION_ENABLED] ?: true }
    val remindOnDueDay: Flow<Boolean> = context.dataStore.data.map { it[REMIND_ON_DUE_DAY] ?: true }
    val darkMode: Flow<Boolean> = context.dataStore.data.map { it[DARK_MODE] ?: false }
    val pinEnabled: Flow<Boolean> = context.dataStore.data.map { it[PIN_ENABLED] ?: false }
    val pinHash: Flow<String?> = context.dataStore.data.map { it[PIN_HASH] }
    val biometricEnabled: Flow<Boolean> = context.dataStore.data.map { it[BIOMETRIC_ENABLED] ?: false }
    val userName: Flow<String> = context.dataStore.data.map { it[USER_NAME] ?: "امید" }
    val userIcon: Flow<String> = context.dataStore.data.map { it[USER_ICON] ?: "avatar_person" }
    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { it[NOTIFICATIONS_ENABLED] ?: true }

    suspend fun setReminderDays(days: Int) = edit { it[REMINDER_DAYS] = days }
    suspend fun setReminderHour(hour: Int) = edit { it[REMINDER_HOUR] = hour.coerceIn(0, 23) }
    suspend fun setNotificationSound(soundId: String) = edit { it[NOTIFICATION_SOUND] = soundId }
    suspend fun setVibrationEnabled(enabled: Boolean) = edit { it[VIBRATION_ENABLED] = enabled }
    suspend fun setRemindOnDueDay(enabled: Boolean) = edit { it[REMIND_ON_DUE_DAY] = enabled }
    suspend fun setDarkMode(enabled: Boolean) = edit { it[DARK_MODE] = enabled }
    suspend fun setPinEnabled(enabled: Boolean) = edit { it[PIN_ENABLED] = enabled }
    suspend fun setPinHash(hash: String) = edit { it[PIN_HASH] = hash }
    suspend fun setBiometricEnabled(enabled: Boolean) = edit { it[BIOMETRIC_ENABLED] = enabled }
    suspend fun setUserName(name: String) = edit { it[USER_NAME] = name }
    suspend fun setUserIcon(icon: String) = edit { it[USER_ICON] = icon }
    suspend fun setNotificationsEnabled(enabled: Boolean) = edit { it[NOTIFICATIONS_ENABLED] = enabled }

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        context.dataStore.edit { block(it) }
    }
}
