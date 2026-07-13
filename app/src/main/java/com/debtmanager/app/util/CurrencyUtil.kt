package com.debtmanager.app.util

object CurrencyUtil {
    fun format(amount: Long): String {
        val formatted = amount.toString().reversed().chunked(3).joinToString(",").reversed()
        return "${PersianDateUtil.toPersianDigits(formatted)} ریال"
    }

    fun formatWithoutUnit(amount: Long): String {
        val formatted = amount.toString().reversed().chunked(3).joinToString(",").reversed()
        return PersianDateUtil.toPersianDigits(formatted)
    }

    fun parse(input: String): Long? {
        val digits = input.filter { it.isDigit() || it in '۰'..'۹' }
            .map { if (it in '۰'..'۹') '0' + (it - '۰') else it }
            .joinToString("")
        return digits.toLongOrNull()
    }
}
