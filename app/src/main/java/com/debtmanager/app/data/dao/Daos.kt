package com.debtmanager.app.data.dao

import androidx.room.*
import com.debtmanager.app.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanDao {
    @Query("SELECT * FROM loans ORDER BY id DESC")
    fun getAllLoans(): Flow<List<Loan>>

    @Query("SELECT * FROM loans WHERE id = :id")
    suspend fun getLoanById(id: Long): Loan?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(loan: Loan): Long

    @Update
    suspend fun update(loan: Loan)

    @Delete
    suspend fun delete(loan: Loan)

    @Query("SELECT * FROM loan_installments WHERE loanId = :loanId ORDER BY dueDate ASC")
    fun getInstallments(loanId: Long): Flow<List<LoanInstallment>>

    @Query("SELECT * FROM loan_installments WHERE loanId = :loanId ORDER BY dueDate ASC")
    suspend fun getInstallmentsList(loanId: Long): List<LoanInstallment>

    @Query("SELECT * FROM loan_installments WHERE isPaid = 0 ORDER BY dueDate ASC")
    fun getUnpaidInstallments(): Flow<List<LoanInstallment>>

    @Query("SELECT * FROM loan_installments WHERE isPaid = 0 AND dueDate < :now ORDER BY dueDate ASC")
    fun getOverdueInstallments(now: Long): Flow<List<LoanInstallment>>

    @Query("SELECT * FROM loan_installments WHERE isPaid = 0 AND dueDate >= :start AND dueDate <= :end ORDER BY dueDate ASC")
    fun getInstallmentsInRange(start: Long, end: Long): Flow<List<LoanInstallment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstallment(installment: LoanInstallment): Long

    @Update
    suspend fun updateInstallment(installment: LoanInstallment)

    @Query("SELECT * FROM loan_installments WHERE id = :id")
    suspend fun getInstallmentById(id: Long): LoanInstallment?

    @Query("DELETE FROM loan_installments WHERE loanId = :loanId")
    suspend fun deleteInstallmentsForLoan(loanId: Long)

    @Query("DELETE FROM loan_installments")
    suspend fun deleteAllInstallments()

    @Query("DELETE FROM loans")
    suspend fun deleteAllLoans()
}

@Dao
interface CheckDao {
    @Query("SELECT * FROM checks ORDER BY dueDate ASC")
    fun getAllChecks(): Flow<List<CheckEntity>>

    @Query("SELECT * FROM checks WHERE id = :id")
    suspend fun getCheckById(id: Long): CheckEntity?

    @Query("SELECT * FROM checks WHERE status = :status ORDER BY dueDate ASC")
    fun getChecksByStatus(status: String): Flow<List<CheckEntity>>

    @Query("SELECT * FROM checks WHERE status = 'PENDING' AND dueDate >= :start AND dueDate <= :end ORDER BY dueDate ASC")
    fun getPendingChecksInRange(start: Long, end: Long): Flow<List<CheckEntity>>

    @Query("SELECT * FROM checks WHERE status = 'PENDING' AND dueDate < :now ORDER BY dueDate ASC")
    fun getOverdueChecks(now: Long): Flow<List<CheckEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(check: CheckEntity): Long

    @Update
    suspend fun update(check: CheckEntity)

    @Delete
    suspend fun delete(check: CheckEntity)

    @Query("DELETE FROM checks")
    suspend fun deleteAll()
}

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts ORDER BY date DESC")
    fun getAllDebts(): Flow<List<Debt>>

    @Query("SELECT * FROM debts ORDER BY date DESC")
    suspend fun getAllDebtsOnce(): List<Debt>

    @Query("SELECT * FROM debts WHERE id = :id")
    suspend fun getDebtById(id: Long): Debt?

    @Query("SELECT * FROM debts WHERE paidAmount < totalAmount ORDER BY date DESC")
    fun getActiveDebts(): Flow<List<Debt>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(debt: Debt): Long

    @Update
    suspend fun update(debt: Debt)

    @Delete
    suspend fun delete(debt: Debt)

    @Query("DELETE FROM debts")
    suspend fun deleteAll()
}

@Dao
interface RecurringPaymentDao {
    @Query("SELECT * FROM recurring_payments ORDER BY nextDueDate ASC")
    fun getAll(): Flow<List<RecurringPayment>>

    @Query("SELECT * FROM recurring_payments WHERE id = :id")
    suspend fun getById(id: Long): RecurringPayment?

    @Query("SELECT * FROM recurring_payments WHERE nextDueDate >= :start AND nextDueDate <= :end ORDER BY nextDueDate ASC")
    fun getInRange(start: Long, end: Long): Flow<List<RecurringPayment>>

    @Query("SELECT * FROM recurring_payments WHERE nextDueDate < :now ORDER BY nextDueDate ASC")
    fun getOverdue(now: Long): Flow<List<RecurringPayment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: RecurringPayment): Long

    @Update
    suspend fun update(payment: RecurringPayment)

    @Delete
    suspend fun delete(payment: RecurringPayment)

    @Query("DELETE FROM recurring_payments")
    suspend fun deleteAll()
}

@Dao
interface PaymentHistoryDao {
    @Query("SELECT * FROM payment_history ORDER BY date DESC")
    fun getAll(): Flow<List<PaymentHistory>>

    @Query("SELECT * FROM payment_history WHERE date >= :start AND date <= :end ORDER BY date DESC")
    fun getInRange(start: Long, end: Long): Flow<List<PaymentHistory>>

    @Query("SELECT * FROM payment_history WHERE type = :type ORDER BY date DESC")
    fun getByType(type: String): Flow<List<PaymentHistory>>

    @Query("SELECT * FROM payment_history WHERE description LIKE '%' || :query || '%' OR type LIKE '%' || :query || '%' ORDER BY date DESC")
    fun search(query: String): Flow<List<PaymentHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: PaymentHistory): Long

    @Delete
    suspend fun delete(payment: PaymentHistory)

    @Query("DELETE FROM payment_history")
    suspend fun deleteAll()

    @Query("DELETE FROM payment_history WHERE type = :type AND referenceId = :referenceId")
    suspend fun deleteByReference(type: String, referenceId: Long)

    @Query("DELETE FROM payment_history WHERE type = :type AND referenceId IN (:referenceIds)")
    suspend fun deleteByReferences(type: String, referenceIds: List<Long>)
}


@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getAll(): Flow<List<Contact>>

    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun getById(id: Long): Contact?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: Contact): Long

    @Update
    suspend fun update(contact: Contact)

    @Delete
    suspend fun delete(contact: Contact)

    @Query("DELETE FROM contacts")
    suspend fun deleteAll()
}

@Dao
interface BankAccountDao {
    @Query("SELECT * FROM bank_accounts ORDER BY isDefault DESC, name ASC")
    fun getAll(): Flow<List<BankAccount>>

    @Query("SELECT * FROM bank_accounts ORDER BY isDefault DESC, name ASC")
    suspend fun getAllOnce(): List<BankAccount>

    @Query("SELECT * FROM bank_accounts WHERE id = :id")
    suspend fun getById(id: Long): BankAccount?

    @Query("SELECT * FROM bank_accounts WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefault(): BankAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: BankAccount): Long

    @Update
    suspend fun update(account: BankAccount)

    @Delete
    suspend fun delete(account: BankAccount)

    @Query("UPDATE bank_accounts SET balance = balance + :delta WHERE id = :id")
    suspend fun adjustBalance(id: Long, delta: Long)

    @Query("DELETE FROM bank_accounts")
    suspend fun deleteAll()
}

@Dao
interface AccountTransactionDao {
    @Query("SELECT * FROM account_transactions ORDER BY date DESC")
    fun getAll(): Flow<List<AccountTransaction>>

    @Query("SELECT * FROM account_transactions WHERE accountId = :accountId ORDER BY date DESC")
    fun getByAccount(accountId: Long): Flow<List<AccountTransaction>>

    @Query("SELECT * FROM account_transactions WHERE date >= :start AND date <= :end ORDER BY date DESC")
    fun getInRange(start: Long, end: Long): Flow<List<AccountTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tx: AccountTransaction): Long

    @Update
    suspend fun update(tx: AccountTransaction)

    @Delete
    suspend fun delete(tx: AccountTransaction)

    @Query("DELETE FROM account_transactions")
    suspend fun deleteAll()
}


@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAll(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE date >= :start AND date <= :end ORDER BY date DESC")
    fun getInRange(start: Long, end: Long): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE member = :member ORDER BY date DESC")
    fun getByMember(member: String): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getById(id: Long): Expense?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: Expense): Long

    @Update
    suspend fun update(expense: Expense)

    @Delete
    suspend fun delete(expense: Expense)

    @Query("DELETE FROM expenses")
    suspend fun deleteAll()
}
