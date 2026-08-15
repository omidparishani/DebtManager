package com.debtmanager.app.worker

import android.content.Context
import androidx.work.*
import com.debtmanager.app.data.BackupManager
import com.debtmanager.app.data.SettingsRepository
import com.debtmanager.app.data.database.AppDatabase
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class BackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val settings = SettingsRepository(applicationContext)
            val enabled = settings.autoBackupEnabled.first()
            if (!enabled) return Result.success()

            val db = AppDatabase.getInstance(applicationContext)
            val manager = BackupManager(applicationContext, db)
            val file = manager.exportScheduledBackup()
            if (file != null) Result.success() else Result.retry()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_WORK = "scheduled_auto_backup"

        fun schedule(context: Context, intervalHours: Long) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<BackupWorker>(
                intervalHours.coerceAtLeast(6), TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK)
        }

        fun scheduleFromSettings(context: Context, enabled: Boolean, intervalHours: Long) {
            if (enabled) schedule(context, intervalHours) else cancel(context)
        }
    }
}
