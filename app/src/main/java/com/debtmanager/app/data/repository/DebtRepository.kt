package com.debtmanager.app.data.repository

import com.debtmanager.app.data.dao.*
import com.debtmanager.app.data.entity.*
import com.debtmanager.app.util.PersianDateUtil
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class DebtRepository(
    private val loanDao: LoanDao,
    private val checkDao: CheckDao,
    private val debtDao: DebtDao,
    private val recurringDao: RecurringPaymentDao,
    private val historyDao: PaymentHistoryDao
) {
    // Loans
    fun getAllLoans() = loanDao.getAllLoans()
    suspend fun getLoan(id: Long) = loanDao.getLoanById(id)
    fun getInstallments(loanId: Long) = loanDao.getInstallments(loanId)

    suspend fun addLoan(
        title: String,
        totalAmount: Long,
        installmentCount: Int,
        installmentAmount: Long,
        startDate: Long,
        paymentDayOfMonth: Int,
        notes: String = ""
    ): Long {
        val loanId = loanDao.insert(
            Loan(
                title = title,
                totalAmount = totalAmount,
                installmentCount = installmentCount,
                installmentAmount = installmentAmount,
                startDate = startDate,
                paymentDayOfMonth = paymentDayOfMonth,
                notes = notes
            )
        )
        generateInstallments(loanId, startDate, paymentDayOfMonth, installmentCount, installmentAmount)
        return loanId
    }

    suspend fun updateLoan(loan: Loan) = loanDao.update(loan)

    suspend fun deleteLoan(loan: Loan) {
        loanDao.deleteInstallmentsForLoan(loan.id)
        loanDao.delete(loan)
    }

    private suspend fun generateInstallments(
        loanId: Long,
        startDate: Long,
        paymentDay: Int,
        count: Int,
        amount: Long
    ) {
        var dueDate = startDate
        repeat(count) {
            loanDao.insertInstallment(
                LoanInstallment(loanId = loanId, dueDate = dueDate, amount = amount)
            )
            dueDate = PersianDateUtil.addMonths(dueDate, 1)
            val j = PersianDateUtil.fromTimestamp(dueDate)
            dueDate = PersianDateUtil.toTimestamp(j.year, j.month, paymentDay.coerceIn(1, 29))
        }
    }

    suspend fun payInstallment(installment: LoanInstallment, paidAmount: Long, paidDate: Long) {
        val updated = installment.copy(
            isPaid = true,
            paidDate = paidDate,
            paidAmount = paidAmount
        )
        loanDao.updateInstallment(updated)
        val loan = loanDao.getLoanById(installment.loanId) ?: return
        loanDao.update(loan.copy(paidCount = loan.paidCount + 1))
        historyDao.insert(
            PaymentHistory(
                type = PaymentType.LOAN.name,
                referenceId = installment.id,
                amount = paidAmount,
                date = paidDate,
                description = "پرداخت قسط وام: ${loan.title}"
            )
        )
    }

    suspend fun getInstallment(id: Long) = loanDao.getInstallmentById(id)

    fun getUnpaidInstallments() = loanDao.getUnpaidInstallments()
    fun getOverdueInstallments() = loanDao.getOverdueInstallments(System.currentTimeMillis())
    fun getInstallmentsInRange(start: Long, end: Long) = loanDao.getInstallmentsInRange(start, end)

    // Checks
    fun getAllChecks() = checkDao.getAllChecks()
    suspend fun getCheck(id: Long) = checkDao.getCheckById(id)

    suspend fun addCheck(check: CheckEntity): Long = checkDao.insert(check)
    suspend fun updateCheck(check: CheckEntity) = checkDao.update(check)
    suspend fun deleteCheck(check: CheckEntity) = checkDao.delete(check)

    suspend fun collectCheck(check: CheckEntity, date: Long) {
        checkDao.update(check.copy(status = CheckStatus.COLLECTED.name))
        historyDao.insert(
            PaymentHistory(
                type = PaymentType.CHECK.name,
                referenceId = check.id,
                amount = check.amount,
                date = date,
                description = "وصول چک: ${check.payee}"
            )
        )
    }

    fun getPendingChecksInRange(start: Long, end: Long) = checkDao.getPendingChecksInRange(start, end)
    fun getOverdueChecks() = checkDao.getOverdueChecks(System.currentTimeMillis())

    // Debts
    fun getAllDebts() = debtDao.getAllDebts()
    fun getActiveDebts() = debtDao.getActiveDebts()
    suspend fun getDebt(id: Long) = debtDao.getDebtById(id)

    suspend fun addDebt(debt: Debt): Long = debtDao.insert(debt)
    suspend fun updateDebt(debt: Debt) = debtDao.update(debt)
    suspend fun deleteDebt(debt: Debt) = debtDao.delete(debt)

    suspend fun payDebt(debt: Debt, amount: Long, date: Long) {
        val newPaid = (debt.paidAmount + amount).coerceAtMost(debt.totalAmount)
        debtDao.update(debt.copy(paidAmount = newPaid))
        historyDao.insert(
            PaymentHistory(
                type = PaymentType.DEBT.name,
                referenceId = debt.id,
                amount = amount,
                date = date,
                description = "پرداخت بدهی: ${debt.creditorName}"
            )
        )
    }

    // Recurring
    fun getAllRecurring() = recurringDao.getAll()
    suspend fun getRecurring(id: Long) = recurringDao.getById(id)
    fun getRecurringInRange(start: Long, end: Long) = recurringDao.getInRange(start, end)
    fun getOverdueRecurring() = recurringDao.getOverdue(System.currentTimeMillis())

    suspend fun addRecurring(payment: RecurringPayment): Long = recurringDao.insert(payment)
    suspend fun updateRecurring(payment: RecurringPayment) = recurringDao.update(payment)
    suspend fun deleteRecurring(payment: RecurringPayment) = recurringDao.delete(payment)

    suspend fun markRecurringPaid(payment: RecurringPayment, paidDate: Long) {
        val freq = PaymentFrequency.entries.find { it.name == payment.frequency } ?: PaymentFrequency.MONTHLY
        val nextDue = PersianDateUtil.addMonths(payment.nextDueDate, freq.months)
        recurringDao.update(
            payment.copy(lastPaidDate = paidDate, nextDueDate = nextDue)
        )
        historyDao.insert(
            PaymentHistory(
                type = PaymentType.RECURRING.name,
                referenceId = payment.id,
                amount = payment.amount,
                date = paidDate,
                description = "پرداخت قسط دوره‌ای: ${payment.title}"
            )
        )
    }

    // History
    fun getPaymentHistory() = historyDao.getAll()
    fun searchHistory(query: String) = historyDao.search(query)
    fun getHistoryByType(type: String) = historyDao.getByType(type)
    fun getHistoryInRange(start: Long, end: Long) = historyDao.getInRange(start, end)

    suspend fun getDashboardData(): DashboardData {
        val now = System.currentTimeMillis()
        val monthStart = PersianDateUtil.startOfMonth(now)
        val monthEnd = PersianDateUtil.endOfMonth(now)
        val upcomingEnd = PersianDateUtil.addDays(now, 30)

        val overdueInstallments = loanDao.getOverdueInstallments(now)
        val monthInstallments = loanDao.getInstallmentsInRange(monthStart, monthEnd)
        val overdueChecks = checkDao.getOverdueChecks(now)
        val monthChecks = checkDao.getPendingChecksInRange(monthStart, monthEnd)
        val activeDebts = debtDao.getActiveDebts()
        val overdueRecurring = recurringDao.getOverdue(now)
        val monthRecurring = recurringDao.getInRange(monthStart, monthEnd)
        val upcomingRecurring = recurringDao.getInRange(now, upcomingEnd)
        val monthHistory = historyDao.getInRange(monthStart, monthEnd)

        return DashboardData(
            monthStart = monthStart,
            monthEnd = monthEnd,
            now = now
        )
    }
}

data class DashboardData(
    val monthStart: Long,
    val monthEnd: Long,
    val now: Long
)

data class UpcomingItem(
    val title: String,
    val amount: Long,
    val dueDate: Long,
    val type: PaymentType,
    val referenceId: Long,
    val status: InstallmentStatus
)

fun getInstallmentStatus(dueDate: Long, isPaid: Boolean, now: Long = System.currentTimeMillis()): InstallmentStatus {
    if (isPaid) return InstallmentStatus.PAID
    val today = PersianDateUtil.startOfDay(now)
    val due = PersianDateUtil.startOfDay(dueDate)
    return when {
        due < today -> InstallmentStatus.OVERDUE
        else -> InstallmentStatus.UPCOMING
    }
}
