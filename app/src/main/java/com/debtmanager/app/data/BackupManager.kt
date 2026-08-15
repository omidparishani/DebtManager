package com.debtmanager.app.data

import android.content.Context
import android.os.Environment
import com.debtmanager.app.data.database.AppDatabase
import com.debtmanager.app.data.entity.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupData(
    val loans: List<Loan> = emptyList(),
    val installments: List<LoanInstallment> = emptyList(),
    val checks: List<CheckEntity> = emptyList(),
    val debts: List<Debt> = emptyList(),
    val recurringPayments: List<RecurringPayment> = emptyList(),
    val paymentHistory: List<PaymentHistory> = emptyList(),
    val contacts: List<Contact> = emptyList(),
    val bankAccounts: List<BankAccount> = emptyList(),
    val accountTransactions: List<AccountTransaction> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val version: Int = 2,
    val exportDate: Long = System.currentTimeMillis()
)

class BackupManager(private val context: Context, private val db: AppDatabase) {

    private val gson = Gson()

    suspend fun collectBackupData(): BackupData = withContext(Dispatchers.IO) {
        val loans = db.loanDao().getAllLoans().first()
        val installments = loans.flatMap { db.loanDao().getInstallmentsList(it.id) }
        BackupData(
            loans = loans,
            installments = installments,
            checks = db.checkDao().getAllChecks().first(),
            debts = db.debtDao().getAllDebts().first(),
            recurringPayments = db.recurringPaymentDao().getAll().first(),
            paymentHistory = db.paymentHistoryDao().getAll().first(),
            contacts = db.contactDao().getAll().first(),
            bankAccounts = db.bankAccountDao().getAll().first(),
            accountTransactions = db.accountTransactionDao().getAll().first(),
            expenses = try { db.expenseDao().getAll().first() } catch (_: Exception) { emptyList() },
            version = 2,
            exportDate = System.currentTimeMillis()
        )
    }

    suspend fun exportToJson(): File = withContext(Dispatchers.IO) {
        val backup = collectBackupData()
        val file = File(context.cacheDir, "debt_manager_backup_${System.currentTimeMillis()}.json")
        FileWriter(file).use { gson.toJson(backup, it) }
        file
    }

    /** پشتیبان زمان‌بندی‌شده در پوشه Documents/DebtManagerBackups */
    suspend fun exportScheduledBackup(): File? = withContext(Dispatchers.IO) {
        try {
            val backup = collectBackupData()
            val dir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                "DebtManagerBackups"
            )
            if (!dir.exists()) dir.mkdirs()
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(dir, "auto_backup_$stamp.json")
            FileWriter(file).use { gson.toJson(backup, it) }
            // نگه داشتن حداکثر ۱۰ فایل آخر
            dir.listFiles()?.filter { it.name.startsWith("auto_backup_") }
                ?.sortedByDescending { it.lastModified() }
                ?.drop(10)
                ?.forEach { it.delete() }
            file
        } catch (_: Exception) {
            null
        }
    }

    suspend fun importFromJson(file: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val backup: BackupData = FileReader(file).use { gson.fromJson(it, BackupData::class.java) }

            // پاک‌سازی و وارد کردن
            db.clearAllTables()

            backup.bankAccounts.forEach { db.bankAccountDao().insert(it.copy(id = 0)) }
            backup.contacts.forEach { db.contactDao().insert(it.copy(id = 0)) }
            backup.loans.forEach { loan ->
                val newId = db.loanDao().insert(loan.copy(id = 0))
                backup.installments.filter { it.loanId == loan.id }.forEach {
                    db.loanDao().insertInstallment(it.copy(id = 0, loanId = newId))
                }
            }
            backup.checks.forEach { db.checkDao().insert(it.copy(id = 0)) }
            backup.debts.forEach { db.debtDao().insert(it.copy(id = 0)) }
            backup.recurringPayments.forEach { db.recurringPaymentDao().insert(it.copy(id = 0)) }
            backup.paymentHistory.forEach { db.paymentHistoryDao().insert(it.copy(id = 0)) }
            backup.accountTransactions.forEach {
                try { db.accountTransactionDao().insert(it.copy(id = 0)) } catch (_: Exception) {}
            }
            backup.expenses.forEach {
                try { db.expenseDao().insert(it.copy(id = 0)) } catch (_: Exception) {}
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
