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
import com.debtmanager.app.data.SettingsRepository
import com.debtmanager.app.data.database.AppDatabase
import com.debtmanager.app.data.entity.CheckStatus
import com.debtmanager.app.util.CurrencyUtil
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

        createChannel()
        showNotification(id, title, amount, dueDate)
        return Result.success()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "یادآوری پرداخت",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "یادآوری سررسید اقساط و بدهی‌ها" }
            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(id: Int, title: String, amount: Long, dueDate: Long) {
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            applicationContext, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("یادآوری پرداخت")
            .setContentText("$title - ${CurrencyUtil.format(amount)} - سررسید: ${PersianDateUtil.formatShort(dueDate)}")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(applicationContext).notify(id, notification)
        } catch (_: SecurityException) { }
    }

    companion object {
        const val CHANNEL_ID = "debt_reminders"
        const val KEY_TITLE = "title"
        const val KEY_AMOUNT = "amount"
        const val KEY_DUE_DATE = "due_date"
        const val KEY_NOTIFICATION_ID = "notification_id"

        fun schedule(
            context: Context,
            notificationId: Int,
            title: String,
            amount: Long,
            dueDate: Long,
            reminderDaysBefore: Int
        ) {
            val reminderTime = PersianDateUtil.addDays(dueDate, -reminderDaysBefore)
            val delay = reminderTime - System.currentTimeMillis()
            if (delay <= 0) return

            val data = workDataOf(
                KEY_TITLE to title,
                KEY_AMOUNT to amount,
                KEY_DUE_DATE to dueDate,
                KEY_NOTIFICATION_ID to notificationId
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
        }
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
        val db = AppDatabase.getInstance(context)
        val loanDao = db.loanDao()

        loanDao.getUnpaidInstallments().first().forEach { inst ->
            val loan = loanDao.getLoanById(inst.loanId)
            ReminderWorker.schedule(
                context, inst.id.toInt(), "قسط وام: ${loan?.title ?: ""}",
                inst.amount, inst.dueDate, reminderDays
            )
        }

        db.checkDao().getAllChecks().first()
            .filter { it.status == CheckStatus.PENDING.name }
            .forEach { check ->
                scheduleForCheck(context, check.id, check.payee, check.amount, check.dueDate, reminderDays)
            }

        db.recurringPaymentDao().getAll().first().forEach { r ->
            scheduleForRecurring(context, r.id, r.title, r.amount, r.nextDueDate, reminderDays)
        }
    }

    fun scheduleForInstallment(context: Context, id: Long, title: String, amount: Long, dueDate: Long, days: Int) {
        ReminderWorker.schedule(context, id.toInt(), title, amount, dueDate, days)
    }

    fun scheduleForCheck(context: Context, id: Long, payee: String, amount: Long, dueDate: Long, days: Int) {
        ReminderWorker.schedule(context, (id + 10000).toInt(), "چک: $payee", amount, dueDate, days)
    }

    fun scheduleForRecurring(context: Context, id: Long, title: String, amount: Long, dueDate: Long, days: Int) {
        ReminderWorker.schedule(context, (id + 20000).toInt(), title, amount, dueDate, days)
    }
}
