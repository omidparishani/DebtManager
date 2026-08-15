package com.debtmanager.app

import android.app.Application
import com.debtmanager.app.data.SettingsRepository
import com.debtmanager.app.data.database.AppDatabase
import com.debtmanager.app.worker.BackupWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DebtManagerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppDatabase.getInstance(this)
        // زمان‌بندی پشتیبان‌گیری خودکار طبق تنظیمات
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = SettingsRepository(this@DebtManagerApp)
                val enabled = settings.autoBackupEnabled.first()
                val hours = settings.autoBackupIntervalHours.first().toLong()
                BackupWorker.scheduleFromSettings(this@DebtManagerApp, enabled, hours)
            } catch (_: Exception) {
            }
        }
    }
}
