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
import com.debtmanager.app.worker.ReminderWorker
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
    val themeColor = settings.themeColor.stateIn(viewModelScope, SharingStarted.Eagerly, "teal")
    val autoBackupEnabled = settings.autoBackupEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val autoBackupIntervalHours = settings.autoBackupIntervalHours.stateIn(viewModelScope, SharingStarted.Eagerly, 24)
    val reminderDays = settings.reminderDays.stateIn(viewModelScope, SharingStarted.Eagerly, 3)
    val reminderHour = settings.reminderHour.stateIn(viewModelScope, SharingStarted.Eagerly, 9)
    val notificationSound = settings.notificationSound.stateIn(viewModelScope, SharingStarted.Eagerly, "default")
    val vibrationEnabled = settings.vibrationEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val remindOnDueDay = settings.remindOnDueDay.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val pinEnabled = settings.pinEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val pinHash = settings.pinHash.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val biometricEnabled = settings.biometricEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val userName = settings.userName.stateIn(viewModelScope, SharingStarted.Eagerly, "امید")
    val userIcon = settings.userIcon.stateIn(viewModelScope, SharingStarted.Eagerly, "avatar_person")
    val notificationsEnabled = settings.notificationsEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val now = MutableStateFlow(System.currentTimeMillis())

    val loans = repository.getAllLoans().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val checks = repository.getAllChecks().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val debts = repository.getAllDebts().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val recurring = repository.getAllRecurring().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    val paymentHistory = repository.getPaymentHistory().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // New: Contacts & Bank Accounts
    val contacts = db.contactDao().getAll().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val bankAccounts = db.bankAccountDao().getAll().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val accountTransactions = db.accountTransactionDao().getAll().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val expenses = db.expenseDao().getAll().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())


    private val _dashboardState = MutableStateFlow(DashboardUiState())
    val dashboardState: StateFlow<DashboardUiState> = _dashboardState.asStateFlow()

    private val _selectedMonthMillis = MutableStateFlow(System.currentTimeMillis())
    val selectedMonthMillis: StateFlow<Long> = _selectedMonthMillis.asStateFlow()

    fun setSelectedMonth(millis: Long) {
        _selectedMonthMillis.value = millis
        // trigger rebuild by touching now
        viewModelScope.launch {
            // rebuild will pick up new month via selectedMonthMillis
        }
    }

    fun shiftSelectedMonth(deltaMonths: Int) {
        _selectedMonthMillis.value = PersianDateUtil.addMonths(_selectedMonthMillis.value, deltaMonths)
        lastDashboardInputs?.let { _dashboardState.value = buildDashboard(it) }
    }

    @Volatile
    private var lastDashboardInputs: DashboardInputs? = null

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
            }.collect { inputs ->
                lastDashboardInputs = inputs
                _dashboardState.value = buildDashboard(inputs)
            }
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
        val selected = _selectedMonthMillis.value
        val monthStart = PersianDateUtil.startOfMonth(selected)
        val monthEnd = PersianDateUtil.endOfMonth(selected)
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
                    status = getInstallmentStatus(inst.dueDate, inst.isPaid, currentTime),
                    icon = loan?.icon ?: "account_balance"
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
                    status = getInstallmentStatus(check.dueDate, false, currentTime),
                    icon = check.icon.ifBlank { "receipt" }
                ))
            }
            recurringList.filter { it.nextDueDate in currentTime..upcomingEnd }.forEach { r ->
                add(UpcomingItem(
                    title = r.title,
                    amount = r.amount,
                    dueDate = r.nextDueDate,
                    type = PaymentType.RECURRING,
                    referenceId = r.id,
                    status = getInstallmentStatus(r.nextDueDate, false, currentTime),
                    icon = r.icon.ifBlank { "money" }
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
                    status = InstallmentStatus.OVERDUE,
                    icon = loan?.icon ?: "account_balance"
                ))
            }
            overdueChecks.forEach { check ->
                add(UpcomingItem(
                    title = "چک: ${check.payee}",
                    amount = check.amount,
                    dueDate = check.dueDate,
                    type = PaymentType.CHECK,
                    referenceId = check.id,
                    status = InstallmentStatus.OVERDUE,
                    icon = check.icon.ifBlank { "receipt" }
                ))
            }
            overdueRecurring.forEach { r ->
                add(UpcomingItem(
                    title = r.title,
                    amount = r.amount,
                    dueDate = r.nextDueDate,
                    type = PaymentType.RECURRING,
                    referenceId = r.id,
                    status = InstallmentStatus.OVERDUE,
                    icon = r.icon.ifBlank { "money" }
                ))
            }
            activeDebts.filter { it.paidAmount < it.totalAmount }.forEach { debt ->
                val icon = debt.icon.ifBlank { com.debtmanager.app.util.ItemIcons.defaultForDebtCategory(debt.category) }
                add(UpcomingItem(
                    title = "بدهی: ${debt.creditorName}",
                    amount = debt.totalAmount - debt.paidAmount,
                    dueDate = debt.date,
                    type = PaymentType.DEBT,
                    referenceId = debt.id,
                    status = InstallmentStatus.OVERDUE,
                    icon = icon
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
        icon: String = "account_balance",
        onDone: () -> Unit
    ) = viewModelScope.launch {
        val id = repository.addLoan(title, totalAmount, installmentCount, installmentAmount, startDate, paymentDay, notes, icon)
        val installments = db.loanDao().getInstallmentsList(id)
        val days = reminderDays.value
        installments.forEach { inst ->
            ReminderScheduler.scheduleForInstallment(getApplication(), inst.id, "قسط وام: $title", inst.amount, inst.dueDate, days)
        }
        onDone()
    }

    fun payInstallment(installment: LoanInstallment, amount: Long, date: Long, bankAccountId: Long? = null, onDone: () -> Unit) = viewModelScope.launch {
        repository.payInstallment(installment, amount, date, bankAccountId)
        val loan = repository.getLoan(installment.loanId)
        applyPaymentToAccount(
            bankAccountId, amount, date,
            "پرداخت قسط وام: ${loan?.title ?: ""}",
            "LOAN", installment.id
        )
        onDone()
    }

    fun updateLoan(loan: Loan, onDone: () -> Unit) = viewModelScope.launch {
        repository.updateLoan(loan)
        onDone()
    }

    fun deleteLoan(loan: Loan, onDone: () -> Unit) = viewModelScope.launch {
        val installments = db.loanDao().getInstallmentsList(loan.id)
        repository.deleteLoan(loan)
        installments.forEach { ReminderWorker.cancel(getApplication(), it.id.toInt()) }
        onDone()
    }

    // Check operations
    fun addCheck(check: CheckEntity, onDone: () -> Unit) = viewModelScope.launch {
        val id = repository.addCheck(check)
        ReminderScheduler.scheduleForCheck(
            getApplication(), id, check.payee, check.amount, check.dueDate,
            reminderDays.value, check.description
        )
        onDone()
    }

    fun updateCheck(check: CheckEntity, onDone: () -> Unit) = viewModelScope.launch {
        repository.updateCheck(check)
        ReminderScheduler.scheduleForCheck(
            getApplication(), check.id, check.payee, check.amount, check.dueDate,
            reminderDays.value, check.description
        )
        onDone()
    }

    fun collectCheck(check: CheckEntity, date: Long, bankAccountId: Long? = null, onDone: () -> Unit) = viewModelScope.launch {
        repository.collectCheck(check, date, bankAccountId)
        applyPaymentToAccount(
            bankAccountId, check.amount, date,
            "وصول چک: ${check.payee}",
            "CHECK", check.id
        )
        onDone()
    }

    fun deleteCheck(check: CheckEntity, onDone: () -> Unit) = viewModelScope.launch {
        repository.deleteCheck(check)
        ReminderWorker.cancel(getApplication(), (check.id + 10000).toInt())
        onDone()
    }

    // Debt operations
    fun addDebt(debt: Debt, onDone: () -> Unit) = viewModelScope.launch {
        repository.addDebt(debt)
        onDone()
    }


    /** پرداخت یکجای همه بدهکاری‌های باز یک شخص از یک حساب بانکی */
    fun payAllDebtsForContact(
        contactId: Long,
        contactName: String,
        bankAccountId: Long,
        date: Long = System.currentTimeMillis(),
        onDone: (paidCount: Int, totalAmount: Long) -> Unit = { _, _ -> }
    ) = viewModelScope.launch {
        val all = debts.value.filter { d ->
            !d.isCredit &&
                (d.contactId == contactId || d.creditorName == contactName) &&
                (d.totalAmount - d.paidAmount) > 0
        }
        var total = 0L
        for (debt in all) {
            val remaining = (debt.totalAmount - debt.paidAmount).coerceAtLeast(0)
            if (remaining <= 0) continue
            repository.payDebt(debt, remaining, date, bankAccountId)
            db.bankAccountDao().adjustBalance(bankAccountId, -remaining)
            db.accountTransactionDao().insert(
                AccountTransaction(
                    accountId = bankAccountId,
                    type = "WITHDRAW",
                    amount = remaining,
                    date = date,
                    description = "تسویه کامل بدهی: ${debt.creditorName}",
                    relatedType = "DEBT",
                    relatedId = debt.id
                )
            )
            total += remaining
        }
        onDone(all.size, total)
    }

    fun payDebt(debt: Debt, amount: Long, date: Long, bankAccountId: Long? = null, onDone: () -> Unit) = viewModelScope.launch {
        repository.payDebt(debt, amount, date, bankAccountId)
        // کسر/افزایش موجودی حساب بانکی
        if (bankAccountId != null && amount > 0) {
            if (debt.isCredit) {
                // بستانکاری: پول به حساب ما واریز می‌شود
                db.bankAccountDao().adjustBalance(bankAccountId, amount)
                db.accountTransactionDao().insert(
                    AccountTransaction(
                        accountId = bankAccountId,
                        type = "DEPOSIT",
                        amount = amount,
                        date = date,
                        description = "دریافت بستانکاری: ${debt.creditorName}",
                        relatedType = "CREDIT",
                        relatedId = debt.id
                    )
                )
            } else {
                // بدهکاری: از حساب ما برداشت می‌شود
                db.bankAccountDao().adjustBalance(bankAccountId, -amount)
                db.accountTransactionDao().insert(
                    AccountTransaction(
                        accountId = bankAccountId,
                        type = "WITHDRAW",
                        amount = amount,
                        date = date,
                        description = "پرداخت بدهی: ${debt.creditorName}",
                        relatedType = "DEBT",
                        relatedId = debt.id
                    )
                )
            }
        }
        onDone()
    }

    fun updateDebt(debt: Debt, onDone: () -> Unit) = viewModelScope.launch {
        repository.updateDebt(debt)
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

    fun updateRecurring(payment: RecurringPayment, onDone: () -> Unit) = viewModelScope.launch {
        repository.updateRecurring(payment)
        ReminderScheduler.scheduleForRecurring(
            getApplication(), payment.id, payment.title, payment.amount,
            payment.nextDueDate, reminderDays.value, payment.category
        )
        onDone()
    }

    fun markRecurringPaid(payment: RecurringPayment, date: Long, bankAccountId: Long? = null, onDone: () -> Unit) = viewModelScope.launch {
        repository.markRecurringPaid(payment, date, bankAccountId)
        applyPaymentToAccount(
            bankAccountId, payment.amount, date,
            "پرداخت دوره‌ای: ${payment.title}",
            "RECURRING", payment.id
        )
        onDone()
    }

    fun deleteRecurring(payment: RecurringPayment, onDone: () -> Unit) = viewModelScope.launch {
        repository.deleteRecurring(payment)
        ReminderWorker.cancel(getApplication(), (payment.id + 20000).toInt())
        onDone()
    }

    // Settings
    fun setThemeColor(color: String) = viewModelScope.launch { settings.setThemeColor(color) }
    fun setAutoBackupEnabled(enabled: Boolean) = viewModelScope.launch {
        settings.setAutoBackupEnabled(enabled)
        com.debtmanager.app.worker.BackupWorker.scheduleFromSettings(
            getApplication(), enabled, autoBackupIntervalHours.value.toLong()
        )
    }
    fun setAutoBackupIntervalHours(hours: Int) = viewModelScope.launch {
        settings.setAutoBackupIntervalHours(hours)
        if (autoBackupEnabled.value) {
            com.debtmanager.app.worker.BackupWorker.scheduleFromSettings(
                getApplication(), true, hours.toLong()
            )
        }
    }
    fun setDarkMode(enabled: Boolean) = viewModelScope.launch { settings.setDarkMode(enabled) }
    fun setReminderDays(days: Int) = viewModelScope.launch {
        settings.setReminderDays(days)
        ReminderScheduler.rescheduleAll(getApplication())
    }
    fun setReminderHour(hour: Int) = viewModelScope.launch {
        settings.setReminderHour(hour)
        ReminderScheduler.rescheduleAll(getApplication())
    }
    fun setNotificationSound(soundId: String) = viewModelScope.launch { settings.setNotificationSound(soundId) }
    fun setVibrationEnabled(enabled: Boolean) = viewModelScope.launch { settings.setVibrationEnabled(enabled) }
    fun setRemindOnDueDay(enabled: Boolean) = viewModelScope.launch {
        settings.setRemindOnDueDay(enabled)
        ReminderScheduler.rescheduleAll(getApplication())
    }
    fun setUserName(name: String) = viewModelScope.launch { settings.setUserName(name) }
    fun setUserIcon(icon: String) = viewModelScope.launch { settings.setUserIcon(icon) }
    fun setNotificationsEnabled(enabled: Boolean) = viewModelScope.launch {
        settings.setNotificationsEnabled(enabled)
        if (enabled) ReminderScheduler.rescheduleAll(getApplication())
    }
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

    fun clearAllData(onDone: () -> Unit) = viewModelScope.launch {
        repository.clearAllData()
        onDone()
    }

    fun getInstallments(loanId: Long) = repository.getInstallments(loanId)

    // ========== Contacts ==========
    fun addContact(contact: Contact, onDone: () -> Unit = {}) = viewModelScope.launch {
        db.contactDao().insert(contact)
        onDone()
    }
    fun updateContact(contact: Contact, onDone: () -> Unit = {}) = viewModelScope.launch {
        db.contactDao().update(contact)
        onDone()
    }
    fun deleteContact(contact: Contact, onDone: () -> Unit = {}) = viewModelScope.launch {
        db.contactDao().delete(contact)
        onDone()
    }

    // ========== Bank Accounts ==========
    fun addBankAccount(account: BankAccount, onDone: () -> Unit = {}) = viewModelScope.launch {
        val id = db.bankAccountDao().insert(account)
        if (account.balance != 0L) {
            db.accountTransactionDao().insert(
                AccountTransaction(
                    accountId = id,
                    type = "DEPOSIT",
                    amount = account.balance,
                    date = System.currentTimeMillis(),
                    description = "موجودی اولیه"
                )
            )
        }
        onDone()
    }

    fun deposit(accountId: Long, amount: Long, date: Long, description: String, onDone: () -> Unit = {}) = viewModelScope.launch {
        db.bankAccountDao().adjustBalance(accountId, amount)
        db.accountTransactionDao().insert(
            AccountTransaction(accountId = accountId, type = "DEPOSIT", amount = amount, date = date, description = description)
        )
        onDone()
    }

    fun withdraw(accountId: Long, amount: Long, date: Long, description: String, onDone: () -> Unit = {}) = viewModelScope.launch {
        db.bankAccountDao().adjustBalance(accountId, -amount)
        db.accountTransactionDao().insert(
            AccountTransaction(accountId = accountId, type = "WITHDRAW", amount = amount, date = date, description = description)
        )
        onDone()
    }

    private suspend fun applyPaymentToAccount(
        accountId: Long?,
        amount: Long,
        date: Long,
        description: String,
        relatedType: String,
        relatedId: Long,
        isDeposit: Boolean = false
    ) {
        if (accountId == null || amount <= 0) return
        if (isDeposit) {
            db.bankAccountDao().adjustBalance(accountId, amount)
            db.accountTransactionDao().insert(
                AccountTransaction(
                    accountId = accountId, type = "DEPOSIT", amount = amount, date = date,
                    description = description, relatedType = relatedType, relatedId = relatedId
                )
            )
        } else {
            db.bankAccountDao().adjustBalance(accountId, -amount)
            db.accountTransactionDao().insert(
                AccountTransaction(
                    accountId = accountId, type = "WITHDRAW", amount = amount, date = date,
                    description = description, relatedType = relatedType, relatedId = relatedId
                )
            )
        }
    }

    
    fun addExpense(expense: Expense, onDone: () -> Unit = {}) = viewModelScope.launch {
        val id = db.expenseDao().insert(expense)
        if (expense.accountId != null && expense.amount > 0) {
            applyPaymentToAccount(
                expense.accountId, expense.amount, expense.date,
                "مخارج: ${expense.title}", "EXPENSE", id
            )
        }
        onDone()
    }
    fun updateExpense(expense: Expense, onDone: () -> Unit = {}) = viewModelScope.launch {
        db.expenseDao().update(expense)
        onDone()
    }
    fun deleteExpense(expense: Expense, onDone: () -> Unit = {}) = viewModelScope.launch {
        db.expenseDao().delete(expense)
        onDone()
    }


    fun updateAccountTransaction(
        old: AccountTransaction,
        newAmount: Long,
        newDate: Long,
        newDescription: String,
        onDone: () -> Unit = {}
    ) = viewModelScope.launch {
        // برگشت اثر قبلی
        val sign = if (old.type == "DEPOSIT") -1L else 1L
        db.bankAccountDao().adjustBalance(old.accountId, sign * old.amount)
        // اعمال مبلغ جدید
        val newSign = if (old.type == "DEPOSIT") 1L else -1L
        db.bankAccountDao().adjustBalance(old.accountId, newSign * newAmount)
        db.accountTransactionDao().update(
            old.copy(amount = newAmount, date = newDate, description = newDescription)
        )
        onDone()
    }

    fun deleteAccountTransaction(tx: AccountTransaction, onDone: () -> Unit = {}) = viewModelScope.launch {
        val sign = if (tx.type == "DEPOSIT") -1L else 1L
        db.bankAccountDao().adjustBalance(tx.accountId, sign * tx.amount)
        db.accountTransactionDao().delete(tx)
        onDone()
    }

    /** انتقال بین حساب‌ها با کارمزد (کارمزد از حساب مبدأ کسر می‌شود) */
    fun transferBetweenAccounts(
        fromAccountId: Long,
        toAccountId: Long,
        amount: Long,
        fee: Long,
        date: Long,
        description: String,
        onDone: () -> Unit = {}
    ) = viewModelScope.launch {
        if (fromAccountId == toAccountId || amount <= 0) {
            onDone()
            return@launch
        }
        val totalDebit = amount + fee.coerceAtLeast(0)
        db.bankAccountDao().adjustBalance(fromAccountId, -totalDebit)
        db.bankAccountDao().adjustBalance(toAccountId, amount)
        val desc = description.ifBlank { "انتقال بین حساب‌ها" }
        db.accountTransactionDao().insert(
            AccountTransaction(
                accountId = fromAccountId,
                type = "TRANSFER",
                amount = amount,
                date = date,
                description = "$desc (به حساب مقصد)",
                toAccountId = toAccountId
            )
        )
        if (fee > 0) {
            db.accountTransactionDao().insert(
                AccountTransaction(
                    accountId = fromAccountId,
                    type = "WITHDRAW",
                    amount = fee,
                    date = date,
                    description = "کارمزد انتقال: $desc"
                )
            )
        }
        db.accountTransactionDao().insert(
            AccountTransaction(
                accountId = toAccountId,
                type = "TRANSFER",
                amount = amount,
                date = date,
                description = "$desc (از حساب مبدأ)",
                toAccountId = fromAccountId
            )
        )
        onDone()
    }

    fun deleteBankAccount(account: BankAccount, onDone: () -> Unit = {}) = viewModelScope.launch {
        db.bankAccountDao().delete(account)
        onDone()
    }
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
