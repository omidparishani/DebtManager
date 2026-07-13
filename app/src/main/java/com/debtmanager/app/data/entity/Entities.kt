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
    val paidAmount: Long? = null
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
    val icon: String = "receipt"
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
    val icon: String = ""
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
    val icon: String = "money"
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
    val description: String = ""
)

enum class PaymentType(val label: String) {
    LOAN("وام"),
    CHECK("چک"),
    DEBT("بدهی"),
    RECURRING("قسط دوره‌ای")
}

enum class InstallmentStatus {
    PAID, OVERDUE, UPCOMING
}
