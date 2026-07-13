package com.debtmanager.app.data

import android.content.Context
import com.debtmanager.app.data.database.AppDatabase
import com.debtmanager.app.data.entity.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileReader
import java.io.FileWriter

data class BackupData(
    val loans: List<Loan> = emptyList(),
    val installments: List<LoanInstallment> = emptyList(),
    val checks: List<CheckEntity> = emptyList(),
    val debts: List<Debt> = emptyList(),
    val recurringPayments: List<RecurringPayment> = emptyList(),
    val paymentHistory: List<PaymentHistory> = emptyList(),
    val version: Int = 1,
    val exportDate: Long = System.currentTimeMillis()
)

class BackupManager(private val context: Context, private val db: AppDatabase) {

    private val gson = Gson()

    suspend fun exportToJson(): File = withContext(Dispatchers.IO) {
        val loanDao = db.loanDao()
        val checkDao = db.checkDao()
        val debtDao = db.debtDao()
        val recurringDao = db.recurringPaymentDao()
        val historyDao = db.paymentHistoryDao()

        val loans = loanDao.getAllLoans().first()
        val installments = loans.flatMap { loanDao.getInstallmentsList(it.id) }
        val checks = checkDao.getAllChecks().first()
        val debts = debtDao.getAllDebts().first()
        val recurring = recurringDao.getAll().first()
        val history = historyDao.getAll().first()

        val backup = BackupData(
            loans = loans,
            installments = installments,
            checks = checks,
            debts = debts,
            recurringPayments = recurring,
            paymentHistory = history
        )

        val file = File(context.cacheDir, "debt_manager_backup_${System.currentTimeMillis()}.json")
        FileWriter(file).use { gson.toJson(backup, it) }
        file
    }

    suspend fun importFromJson(file: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val backup: BackupData = FileReader(file).use { gson.fromJson(it, BackupData::class.java) }
            val loanDao = db.loanDao()
            val checkDao = db.checkDao()
            val debtDao = db.debtDao()
            val recurringDao = db.recurringPaymentDao()
            val historyDao = db.paymentHistoryDao()

            db.clearAllTables()

            backup.loans.forEach { loanDao.insert(it) }
            backup.installments.forEach { loanDao.insertInstallment(it) }
            backup.checks.forEach { checkDao.insert(it) }
            backup.debts.forEach { debtDao.insert(it) }
            backup.recurringPayments.forEach { recurringDao.insert(it) }
            backup.paymentHistory.forEach { historyDao.insert(it) }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
