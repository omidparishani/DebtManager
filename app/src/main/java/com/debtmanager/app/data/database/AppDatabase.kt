package com.debtmanager.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.debtmanager.app.data.dao.*
import com.debtmanager.app.data.entity.*

@Database(
    entities = [
        Loan::class,
        LoanInstallment::class,
        CheckEntity::class,
        Debt::class,
        RecurringPayment::class,
        PaymentHistory::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun loanDao(): LoanDao
    abstract fun checkDao(): CheckDao
    abstract fun debtDao(): DebtDao
    abstract fun recurringPaymentDao(): RecurringPaymentDao
    abstract fun paymentHistoryDao(): PaymentHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "debt_manager.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
