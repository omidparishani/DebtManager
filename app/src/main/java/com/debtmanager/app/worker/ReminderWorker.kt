package com.debtmanager.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.debtmanager.app.MainActivity
import com.debtmanager.app.R
import com.debtmanager.app.data.SettingsRepository
import com.debtmanager.app.data.database.AppDatabase
import com.debtmanager.app.data.entity.CheckStatus
import com.debtmanager.app.util.CurrencyUtil
import com.debtmanager.app.util.NotificationSoundHelper
import com.debtmanager.app.util.PersianDateUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE) ?: return Result.failure()
        val amount = inputData.getLong(KEY_AMOUNT, 0)
        val dueDate = inputData.getLong(KEY_DUE_DATE, 0)
        val id = inputData.getInt(KEY_NOTIFICATION_ID, 0)
        val description = inputData.getString(KEY_DESCRIPTION) ?: ""
        val daysBefore = inputData.getInt(KEY_DAYS_BEFORE, 3)

        val settings = SettingsRepository(applicationContext)
        val notificationsEnabled = settings.notificationsEnabled.first()
        if (!notificationsEnabled) return Result.success()

        val userName = settings.userName.first()
        val soundId = settings.notificationSound.first()
        val vibrationEnabled = settings.vibrationEnabled.first()

        createChannel(soundId)
        showNotification(id, userName, title, amount, dueDate, description, daysBefore, soundId, vibrationEnabled)
        return Result.success()
    }

    private fun createChannel(soundId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = NotificationSoundHelper.getUri(applicationContext, soundId)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "یادآوری پرداخت",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "یادآوری سررسید اقساط و بدهی‌ها"
                soundUri?.let {
                    setSound(it, NotificationSoundHelper.audioAttributes())
                }
                enableVibration(true)
            }
            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(
        id: Int,
        userName: String,
        title: String,
        amount: Long,
        dueDate: Long,
        description: String,
        daysBefore: Int,
        soundId: String,
        vibrationEnabled: Boolean
    ) {
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            applicationContext, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dayWord = when (daysBefore) {
            0 -> "امروز"
            1 -> "فردا"
            else -> "${PersianDateUtil.toPersianDigits(daysBefore)} روز دیگه"
        }

        val greeting = if (userName.isNotBlank()) "$userName، هواست باشه!" else "هواست باشه!"
        val detail = buildString {
            append(dayWord)
            append(" ")
            append(title)
            append(" داری")
            append(" — ")
            append(CurrencyUtil.format(amount))
            if (description.isNotBlank()) {
                append(" — ")
                append(description)
            }
            append(" — سررسید: ")
            append(PersianDateUtil.formatShort(dueDate))
        }

        val soundUri = NotificationSoundHelper.getUri(applicationContext, soundId)
        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(greeting)
            .setContentText(detail)
            .setStyle(NotificationCompat.BigTextStyle().bigText(detail))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pending)
            .setAutoCancel(true)

        if (soundUri != null) {
            builder.setSound(soundUri)
        } else {
            builder.setSilent(true)
        }
        if (vibrationEnabled) {
            builder.setDefaults(NotificationCompat.DEFAULT_VIBRATE)
        }

        try {
            NotificationManagerCompat.from(applicationContext).notify(id, builder.build())
        } catch (_: SecurityException) { }
    }

    companion object {
        const val CHANNEL_ID = "debt_reminders"
        const val KEY_TITLE = "title"
        const val KEY_AMOUNT = "amount"
        const val KEY_DUE_DATE = "due_date"
        const val KEY_NOTIFICATION_ID = "notification_id"
        const val KEY_DESCRIPTION = "description"
        const val KEY_DAYS_BEFORE = "days_before"

        fun schedule(
            context: Context,
            notificationId: Int,
            title: String,
            amount: Long,
            dueDate: Long,
            reminderDaysBefore: Int,
            description: String = "",
            reminderHour: Int = 9,
            alsoOnDueDay: Boolean = false
        ) {
            enqueueReminder(
                context, notificationId, title, amount, dueDate,
                reminderDaysBefore, description, reminderHour
            )
            if (alsoOnDueDay && reminderDaysBefore > 0) {
                enqueueReminder(
                    context, notificationId + DUE_DAY_OFFSET, title, amount, dueDate,
                    0, description, reminderHour
                )
            }
        }

        private fun enqueueReminder(
            context: Context,
            notificationId: Int,
            title: String,
            amount: Long,
            dueDate: Long,
            daysBefore: Int,
            description: String,
            reminderHour: Int
        ) {
            val reminderDay = PersianDateUtil.addDays(dueDate, -daysBefore)
            val reminderTime = PersianDateUtil.setTimeOnDay(reminderDay, reminderHour)
            val delay = reminderTime - System.currentTimeMillis()
            if (delay <= 0) return

            val data = workDataOf(
                KEY_TITLE to title,
                KEY_AMOUNT to amount,
                KEY_DUE_DATE to dueDate,
                KEY_NOTIFICATION_ID to notificationId,
                KEY_DESCRIPTION to description,
                KEY_DAYS_BEFORE to daysBefore
            )
            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .addTag("reminder_$notificationId")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "reminder_$notificationId",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun cancel(context: Context, notificationId: Int) {
            WorkManager.getInstance(context).cancelUniqueWork("reminder_$notificationId")
            WorkManager.getInstance(context).cancelUniqueWork("reminder_${notificationId + DUE_DAY_OFFSET}")
        }

        private const val DUE_DAY_OFFSET = 500_000
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pending = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    ReminderScheduler.rescheduleAll(context)
                } finally {
                    pending.finish()
                }
            }
        }
    }
}

object ReminderScheduler {
    suspend fun rescheduleAll(context: Context) {
        val settings = SettingsRepository(context)
        val reminderDays = settings.reminderDays.first()
        val reminderHour = settings.reminderHour.first()
        val remindOnDueDay = settings.remindOnDueDay.first()
        val db = AppDatabase.getInstance(context)
        val loanDao = db.loanDao()

        loanDao.getUnpaidInstallments().first().forEach { inst ->
            val loan = loanDao.getLoanById(inst.loanId)
            ReminderWorker.schedule(
                context, inst.id.toInt(), "قسط وام: ${loan?.title ?: ""}",
                inst.amount, inst.dueDate, reminderDays, loan?.notes ?: "",
                reminderHour, remindOnDueDay
            )
        }

        db.checkDao().getAllChecks().first()
            .filter { it.status == CheckStatus.PENDING.name }
            .forEach { check ->
                scheduleForCheck(context, check.id, check.payee, check.amount, check.dueDate, reminderDays, check.description)
            }

        db.recurringPaymentDao().getAll().first().forEach { r ->
            scheduleForRecurring(context, r.id, r.title, r.amount, r.nextDueDate, reminderDays, r.category)
        }
    }

    suspend fun scheduleForInstallment(
        context: Context, id: Long, title: String, amount: Long, dueDate: Long,
        days: Int, description: String = ""
    ) {
        val settings = SettingsRepository(context)
        ReminderWorker.schedule(
            context, id.toInt(), title, amount, dueDate, days, description,
            settings.reminderHour.first(), settings.remindOnDueDay.first()
        )
    }

    suspend fun scheduleForCheck(
        context: Context, id: Long, payee: String, amount: Long, dueDate: Long,
        days: Int, description: String = ""
    ) {
        val settings = SettingsRepository(context)
        ReminderWorker.schedule(
            context, (id + 10000).toInt(), "چک: $payee", amount, dueDate, days, description,
            settings.reminderHour.first(), settings.remindOnDueDay.first()
        )
    }

    suspend fun scheduleForRecurring(
        context: Context, id: Long, title: String, amount: Long, dueDate: Long,
        days: Int, description: String = ""
    ) {
        val settings = SettingsRepository(context)
        ReminderWorker.schedule(
            context, (id + 20000).toInt(), title, amount, dueDate, days, description,
            settings.reminderHour.first(), settings.remindOnDueDay.first()
        )
    }
}
