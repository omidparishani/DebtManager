package com.debtmanager.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "loans")
data class Loan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val totalAmount: Long,
    val installmentCount: Int,
    val installmentAmount: Long,
    val startDate: Long,
    val paymentDayOfMonth: Int,
    val paidCount: Int = 0,
    val notes: String = "",
    val icon: String = "account_balance"
)

@Entity(
    tableName = "loan_installments",
    foreignKeys = [
        ForeignKey(
            entity = Loan::class,
            parentColumns = ["id"],
            childColumns = ["loanId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("loanId")]
)
data class LoanInstallment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanId: Long,
    val dueDate: Long,
    val amount: Long,
    val isPaid: Boolean = false,
    val paidDate: Long? = null,
    val paidAmount: Long? = null,
    val bankAccountId: Long? = null
)

@Entity(tableName = "checks")
data class CheckEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Long,
    val dueDate: Long,
    val bankName: String,
    val checkNumber: String = "",
    val payee: String,
    val description: String = "",
    val extraInfo: String = "",
    val status: String = CheckStatus.PENDING.name,
    val icon: String = "receipt",
    val bankAccountId: Long? = null
)

enum class CheckStatus {
    PENDING, COLLECTED, BOUNCED, CANCELLED
}

@Entity(tableName = "debts")
data class Debt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val creditorName: String,
    val category: String,
    val totalAmount: Long,
    val paidAmount: Long = 0,
    val date: Long,
    val description: String = "",
    val icon: String = "",
    /** true = بستانکاری (طلب شما از دیگران), false = بدهکاری (بدهی شما به دیگران) */
    val isCredit: Boolean = false,
    val contactId: Long? = null
)

enum class DebtCategory(val label: String) {
    PERSON("فرد"),
    STORE("فروشگاه"),
    OTHER("سایر")
}

@Entity(tableName = "recurring_payments")
data class RecurringPayment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Long,
    val frequency: String,
    val nextDueDate: Long,
    val lastPaidDate: Long? = null,
    val category: String = "",
    val icon: String = "money",
    val bankAccountId: Long? = null
)

enum class PaymentFrequency(val label: String, val months: Int) {
    MONTHLY("ماهانه", 1),
    QUARTERLY("سه‌ماهه", 3),
    YEARLY("سالانه", 12)
}

@Entity(tableName = "payment_history")
data class PaymentHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val referenceId: Long,
    val amount: Long,
    val date: Long,
    val description: String = "",
    val bankAccountId: Long? = null
)

enum class PaymentType(val label: String) {
    LOAN("وام"),
    CHECK("چک"),
    DEBT("بدهی"),
    CREDIT("بستانکاری"),
    RECURRING("قسط دوره‌ای"),
    DEPOSIT("واریز"),
    WITHDRAW("برداشت"),
    TRANSFER("انتقال")
}

enum class InstallmentStatus {
    PAID, OVERDUE, UPCOMING
}

// ========== NEW: Contacts / Persons ==========
@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val notes: String = "",
    val type: String = ContactType.PERSON.name, // PERSON, STORE, OTHER
    val icon: String = "person"
)

enum class ContactType(val label: String) {
    PERSON("فرد"),
    STORE("فروشگاه"),
    OTHER("سایر")
}

// ========== NEW: Bank Accounts ==========
@Entity(tableName = "bank_accounts")
data class BankAccount(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,                 // e.g. "حساب جاری ملت"
    val bankName: String = "",
    val accountNumber: String = "",
    val balance: Long = 0,
    val colorHex: String = "#1976D2",
    val icon: String = "account_balance",
    val isDefault: Boolean = false,
    val notes: String = ""
)

// ========== NEW: Account Transactions (واریز / برداشت / پرداخت) ==========
@Entity(
    tableName = "account_transactions",
    foreignKeys = [
        ForeignKey(
            entity = BankAccount::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("accountId"), Index("date")]
)
data class AccountTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val type: String,                 // DEPOSIT, WITHDRAW, TRANSFER, PAYMENT
    val amount: Long,
    val date: Long,
    val description: String = "",
    val relatedType: String? = null,  // LOAN, DEBT, CHECK, RECURRING ...
    val relatedId: Long? = null,
    val toAccountId: Long? = null     // for transfers
)


// ========== مخارج روزمره ==========
@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Long,
    val date: Long,
    val category: String = ExpenseCategory.OTHER.name,
    /** SELF, SPOUSE, CHILD, FAMILY, SHARED */
    val member: String = ExpenseMember.SELF.name,
    val accountId: Long? = null,
    val notes: String = "",
    val icon: String = "shopping_cart"
)

enum class ExpenseCategory(val label: String) {
    FOOD("خوراک"),
    TRANSPORT("حمل‌ونقل"),
    HOME("خانه و قبوض"),
    HEALTH("درمان"),
    EDUCATION("آموزش"),
    CLOTHES("پوشاک"),
    ENTERTAINMENT("تفریح"),
    SHOPPING("خرید"),
    OTHER("سایر")
}

enum class ExpenseMember(val label: String) {
    SELF("خودم"),
    SPOUSE("همسر"),
    CHILD("فرزند"),
    FAMILY("خانواده"),
    SHARED("مشترک")
}
