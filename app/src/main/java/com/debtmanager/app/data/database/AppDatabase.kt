package com.debtmanager.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 2,
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

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE loans ADD COLUMN icon TEXT NOT NULL DEFAULT 'account_balance'")
                db.execSQL("ALTER TABLE checks ADD COLUMN icon TEXT NOT NULL DEFAULT 'receipt'")
                db.execSQL("ALTER TABLE debts ADD COLUMN icon TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE recurring_payments ADD COLUMN icon TEXT NOT NULL DEFAULT 'money'")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "debt_manager.db"
                ).addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}
