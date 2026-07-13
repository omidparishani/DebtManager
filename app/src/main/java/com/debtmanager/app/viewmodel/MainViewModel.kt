package com.debtmanager.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.debtmanager.app.data.BackupManager
import com.debtmanager.app.data.SettingsRepository
import com.debtmanager.app.data.database.AppDatabase
import com.debtmanager.app.data.entity.*
import com.debtmanager.app.data.repository.DebtRepository
import com.debtmanager.app.data.repository.UpcomingItem
import com.debtmanager.app.data.repository.getInstallmentStatus
import com.debtmanager.app.util.PersianDateUtil
import com.debtmanager.app.worker.ReminderScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    val repository = DebtRepository(
        db.loanDao(), db.checkDao(), db.debtDao(),
        db.recurringPaymentDao(), db.paymentHistoryDao()
    )
    val settings = SettingsRepository(application)
    val backupManager = BackupManager(application, db)

    val darkMode = settings.darkMode.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val reminderDays = settings.reminderDays.stateIn(viewModelScope, SharingStarted.Eagerly, 3)
    val pinEnabled = settings.pinEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val pinHash = settings.pinHash.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val biometricEnabled = settings.biometricEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val now = MutableStateFlow(System.currentTimeMillis())

    val loans = repository.getAllLoans().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val checks = repository.getAllChecks().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val debts = repository.getAllDebts().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val recurring = repository.getAllRecurring().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val paymentHistory = repository.getPaymentHistory().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _dashboardState = MutableStateFlow(DashboardUiState())
    val dashboardState: StateFlow<DashboardUiState> = _dashboardState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                combine(
                    repository.getUnpaidInstallments(),
                    repository.getOverdueInstallments(),
                    repository.getOverdueChecks(),
                    repository.getActiveDebts(),
                    repository.getOverdueRecurring()
                ) { u, oi, oc, ad, orr -> listOf(u, oi, oc, ad, orr) },
                combine(
                    repository.getPaymentHistory(),
                    loans,
                    checks,
                    recurring
                ) { h, l, c, r -> listOf(h, l, c, r) }
            ) { first, second ->
                DashboardInputs(
                    unpaidInstallments = first[0] as List<LoanInstallment>,
                    overdueInstallments = first[1] as List<LoanInstallment>,
                    overdueChecks = first[2] as List<CheckEntity>,
                    activeDebts = first[3] as List<Debt>,
                    overdueRecurring = first[4] as List<RecurringPayment>,
                    history = second[0] as List<PaymentHistory>,
                    loanList = second[1] as List<Loan>,
                    checkList = second[2] as List<CheckEntity>,
                    recurringList = second[3] as List<RecurringPayment>
                )
            }.collect { inputs -> _dashboardState.value = buildDashboard(inputs) }
        }
    }

    private fun buildDashboard(inputs: DashboardInputs): DashboardUiState {
        val unpaidInstallments = inputs.unpaidInstallments
        val overdueInstallments = inputs.overdueInstallments
        val overdueChecks = inputs.overdueChecks
        val activeDebts = inputs.activeDebts
        val overdueRecurring = inputs.overdueRecurring
        val history = inputs.history
        val loanList = inputs.loanList
        val checkList = inputs.checkList
        val recurringList = inputs.recurringList

        val currentTime = System.currentTimeMillis()
        val monthStart = PersianDateUtil.startOfMonth(currentTime)
        val monthEnd = PersianDateUtil.endOfMonth(currentTime)
        val upcomingEnd = PersianDateUtil.addDays(currentTime, 30)

        val monthUnpaidInstallments = unpaidInstallments.filter { it.dueDate in monthStart..monthEnd }
        val monthPendingChecks = checkList.filter {
            it.status == CheckStatus.PENDING.name && it.dueDate in monthStart..monthEnd
        }
        val monthRecurring = recurringList.filter { it.nextDueDate in monthStart..monthEnd }
        val monthDebtRemaining = activeDebts.sumOf { it.totalAmount - it.paidAmount }

        val monthDueTotal = monthUnpaidInstallments.sumOf { it.amount } +
            monthPendingChecks.sumOf { it.amount } +
            monthRecurring.sumOf { it.amount } +
            monthDebtRemaining

        val monthPaidTotal = history.filter { it.date in monthStart..monthEnd }.sumOf { it.amount }
        val unpaidCount = monthUnpaidInstallments.size + monthPendingChecks.size +
            monthRecurring.size + activeDebts.count { it.paidAmount < it.totalAmount }

        val totalDebt = loanList.sumOf { loan ->
            val paid = loan.paidCount * loan.installmentAmount
            loan.totalAmount - paid
        } + checkList.filter { it.status == CheckStatus.PENDING.name }.sumOf { it.amount } +
            activeDebts.sumOf { it.totalAmount - it.paidAmount } +
            recurringList.sumOf { it.amount }

        val totalPaid = history.sumOf { it.amount }

        val upcoming = buildList {
            unpaidInstallments.filter { it.dueDate in currentTime..upcomingEnd }.forEach { inst ->
                val loan = loanList.find { it.id == inst.loanId }
                add(UpcomingItem(
                    title = "قسط وام: ${loan?.title ?: ""}",
                    amount = inst.amount,
                    dueDate = inst.dueDate,
                    type = PaymentType.LOAN,
                    referenceId = inst.id,
                    status = getInstallmentStatus(inst.dueDate, inst.isPaid, currentTime)
                ))
            }
            checkList.filter {
                it.status == CheckStatus.PENDING.name && it.dueDate in currentTime..upcomingEnd
            }.forEach { check ->
                add(UpcomingItem(
                    title = "چک: ${check.payee}",
                    amount = check.amount,
                    dueDate = check.dueDate,
                    type = PaymentType.CHECK,
                    referenceId = check.id,
                    status = getInstallmentStatus(check.dueDate, false, currentTime)
                ))
            }
            recurringList.filter { it.nextDueDate in currentTime..upcomingEnd }.forEach { r ->
                add(UpcomingItem(
                    title = r.title,
                    amount = r.amount,
                    dueDate = r.nextDueDate,
                    type = PaymentType.RECURRING,
                    referenceId = r.id,
                    status = getInstallmentStatus(r.nextDueDate, false, currentTime)
                ))
            }
        }.sortedBy { it.dueDate }

        val overdue = buildList {
            overdueInstallments.forEach { inst ->
                val loan = loanList.find { it.id == inst.loanId }
                add(UpcomingItem(
                    title = "قسط وام: ${loan?.title ?: ""}",
                    amount = inst.amount,
                    dueDate = inst.dueDate,
                    type = PaymentType.LOAN,
                    referenceId = inst.id,
                    status = InstallmentStatus.OVERDUE
                ))
            }
            overdueChecks.forEach { check ->
                add(UpcomingItem(
                    title = "چک: ${check.payee}",
                    amount = check.amount,
                    dueDate = check.dueDate,
                    type = PaymentType.CHECK,
                    referenceId = check.id,
                    status = InstallmentStatus.OVERDUE
                ))
            }
            overdueRecurring.forEach { r ->
                add(UpcomingItem(
                    title = r.title,
                    amount = r.amount,
                    dueDate = r.nextDueDate,
                    type = PaymentType.RECURRING,
                    referenceId = r.id,
                    status = InstallmentStatus.OVERDUE
                ))
            }
            activeDebts.filter { it.paidAmount < it.totalAmount }.forEach { debt ->
                add(UpcomingItem(
                    title = "بدهی: ${debt.creditorName}",
                    amount = debt.totalAmount - debt.paidAmount,
                    dueDate = debt.date,
                    type = PaymentType.DEBT,
                    referenceId = debt.id,
                    status = InstallmentStatus.OVERDUE
                ))
            }
        }.sortedBy { it.dueDate }

        return  DashboardUiState(
            monthDueTotal = monthDueTotal,
            monthPaidTotal = monthPaidTotal,
            unpaidCount = unpaidCount,
            totalDebt = totalDebt,
            totalPaid = totalPaid,
            remaining = totalDebt,
            upcoming = upcoming,
            overdue = overdue
        )
    }

    fun refresh() { now.value = System.currentTimeMillis() }

    // Loan operations
    fun addLoan(
        title: String, totalAmount: Long, installmentCount: Int,
        installmentAmount: Long, startDate: Long, paymentDay: Int, notes: String,
        onDone: () -> Unit
    ) = viewModelScope.launch {
        val id = repository.addLoan(title, totalAmount, installmentCount, installmentAmount, startDate, paymentDay, notes)
        val installments = db.loanDao().getInstallmentsList(id)
        val days = reminderDays.value
        installments.forEach { inst ->
            ReminderScheduler.scheduleForInstallment(getApplication(), inst.id, "قسط وام: $title", inst.amount, inst.dueDate, days)
        }
        onDone()
    }

    fun payInstallment(installment: LoanInstallment, amount: Long, date: Long, onDone: () -> Unit) = viewModelScope.launch {
        repository.payInstallment(installment, amount, date)
        onDone()
    }

    fun deleteLoan(loan: Loan, onDone: () -> Unit) = viewModelScope.launch {
        repository.deleteLoan(loan)
        onDone()
    }

    // Check operations
    fun addCheck(check: CheckEntity, onDone: () -> Unit) = viewModelScope.launch {
        val id = repository.addCheck(check)
        ReminderScheduler.scheduleForCheck(getApplication(), id, check.payee, check.amount, check.dueDate, reminderDays.value)
        onDone()
    }

    fun updateCheck(check: CheckEntity, onDone: () -> Unit) = viewModelScope.launch {
        repository.updateCheck(check)
        onDone()
    }

    fun collectCheck(check: CheckEntity, date: Long, onDone: () -> Unit) = viewModelScope.launch {
        repository.collectCheck(check, date)
        onDone()
    }

    fun deleteCheck(check: CheckEntity, onDone: () -> Unit) = viewModelScope.launch {
        repository.deleteCheck(check)
        onDone()
    }

    // Debt operations
    fun addDebt(debt: Debt, onDone: () -> Unit) = viewModelScope.launch {
        repository.addDebt(debt)
        onDone()
    }

    fun payDebt(debt: Debt, amount: Long, date: Long, onDone: () -> Unit) = viewModelScope.launch {
        repository.payDebt(debt, amount, date)
        onDone()
    }

    fun deleteDebt(debt: Debt, onDone: () -> Unit) = viewModelScope.launch {
        repository.deleteDebt(debt)
        onDone()
    }

    // Recurring operations
    fun addRecurring(payment: RecurringPayment, onDone: () -> Unit) = viewModelScope.launch {
        val id = repository.addRecurring(payment)
        ReminderScheduler.scheduleForRecurring(getApplication(), id, payment.title, payment.amount, payment.nextDueDate, reminderDays.value)
        onDone()
    }

    fun markRecurringPaid(payment: RecurringPayment, date: Long, onDone: () -> Unit) = viewModelScope.launch {
        repository.markRecurringPaid(payment, date)
        val updated = repository.getRecurring(payment.id)
        updated?.let {
            ReminderScheduler.scheduleForRecurring(getApplication(), it.id, it.title, it.amount, it.nextDueDate, reminderDays.value)
        }
        onDone()
    }

    fun deleteRecurring(payment: RecurringPayment, onDone: () -> Unit) = viewModelScope.launch {
        repository.deleteRecurring(payment)
        onDone()
    }

    // Settings
    fun setDarkMode(enabled: Boolean) = viewModelScope.launch { settings.setDarkMode(enabled) }
    fun setReminderDays(days: Int) = viewModelScope.launch { settings.setReminderDays(days) }
    fun setPin(pin: String) = viewModelScope.launch {
        settings.setPinHash(com.debtmanager.app.security.PinManager.hashPin(pin))
        settings.setPinEnabled(true)
    }
    fun disablePin() = viewModelScope.launch { settings.setPinEnabled(false) }
    fun setBiometric(enabled: Boolean) = viewModelScope.launch { settings.setBiometricEnabled(enabled) }

    fun exportBackup(onResult: (File) -> Unit) = viewModelScope.launch {
        onResult(backupManager.exportToJson())
    }

    fun importBackup(file: File, onResult: (Boolean) -> Unit) = viewModelScope.launch {
        onResult(backupManager.importFromJson(file).isSuccess)
    }

    fun getInstallments(loanId: Long) = repository.getInstallments(loanId)
}

private data class DashboardInputs(
    val unpaidInstallments: List<LoanInstallment>,
    val overdueInstallments: List<LoanInstallment>,
    val overdueChecks: List<CheckEntity>,
    val activeDebts: List<Debt>,
    val overdueRecurring: List<RecurringPayment>,
    val history: List<PaymentHistory>,
    val loanList: List<Loan>,
    val checkList: List<CheckEntity>,
    val recurringList: List<RecurringPayment>
)

data class DashboardUiState(
    val monthDueTotal: Long = 0,
    val monthPaidTotal: Long = 0,
    val unpaidCount: Int = 0,
    val totalDebt: Long = 0,
    val totalPaid: Long = 0,
    val remaining: Long = 0,
    val upcoming: List<UpcomingItem> = emptyList(),
    val overdue: List<UpcomingItem> = emptyList()
)
