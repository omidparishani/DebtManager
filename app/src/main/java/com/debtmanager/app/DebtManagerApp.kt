package com.debtmanager.app

import android.app.Application
import com.debtmanager.app.data.database.AppDatabase

class DebtManagerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppDatabase.getInstance(this)
    }
}
