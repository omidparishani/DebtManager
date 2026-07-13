package com.debtmanager.app.util

import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri

data class NotificationSoundOption(val id: String, val label: String)

object NotificationSoundHelper {
    val options = listOf(
        NotificationSoundOption("default", "پیش‌فرض سیستم"),
        NotificationSoundOption("notification", "صدای اعلان"),
        NotificationSoundOption("alarm", "زنگ هشدار"),
        NotificationSoundOption("ringtone", "زنگ تماس"),
        NotificationSoundOption("silent", "بی‌صدا")
    )

    fun labelFor(id: String): String = options.find { it.id == id }?.label ?: "پیش‌فرض"

    fun getUri(context: Context, soundId: String): Uri? = when (soundId) {
        "silent" -> null
        "alarm" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        "ringtone" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        "notification" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    }

    fun audioAttributes(): AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
}
