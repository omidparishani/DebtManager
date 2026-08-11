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
        PaymentHistory::class,
        Contact::class,
        BankAccount::class,
        AccountTransaction::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun loanDao(): LoanDao
    abstract fun checkDao(): CheckDao
    abstract fun debtDao(): DebtDao
    abstract fun recurringPaymentDao(): RecurringPaymentDao
    abstract fun paymentHistoryDao(): PaymentHistoryDao
    abstract fun contactDao(): ContactDao
    abstract fun bankAccountDao(): BankAccountDao
    abstract fun accountTransactionDao(): AccountTransactionDao

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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // New columns on existing tables
                try { db.execSQL("ALTER TABLE debts ADD COLUMN isCredit INTEGER NOT NULL DEFAULT 0") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE debts ADD COLUMN contactId INTEGER") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE loan_installments ADD COLUMN bankAccountId INTEGER") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE checks ADD COLUMN bankAccountId INTEGER") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE recurring_payments ADD COLUMN bankAccountId INTEGER") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE payment_history ADD COLUMN bankAccountId INTEGER") } catch (_: Exception) {}

                // New tables
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS contacts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        phone TEXT NOT NULL DEFAULT '',
                        notes TEXT NOT NULL DEFAULT '',
                        type TEXT NOT NULL DEFAULT 'PERSON',
                        icon TEXT NOT NULL DEFAULT 'person'
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS bank_accounts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        bankName TEXT NOT NULL DEFAULT '',
                        accountNumber TEXT NOT NULL DEFAULT '',
                        balance INTEGER NOT NULL DEFAULT 0,
                        colorHex TEXT NOT NULL DEFAULT '#1976D2',
                        icon TEXT NOT NULL DEFAULT 'account_balance',
                        isDefault INTEGER NOT NULL DEFAULT 0,
                        notes TEXT NOT NULL DEFAULT ''
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS account_transactions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        accountId INTEGER NOT NULL,
                        type TEXT NOT NULL,
                        amount INTEGER NOT NULL,
                        date INTEGER NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        relatedType TEXT,
                        relatedId INTEGER,
                        toAccountId INTEGER,
                        FOREIGN KEY(accountId) REFERENCES bank_accounts(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_account_transactions_accountId ON account_transactions(accountId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_account_transactions_date ON account_transactions(date)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "debt_manager.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}
