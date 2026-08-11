package com.debtmanager.app.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

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

    fun formatInput(input: String): String {
        val amount = parse(input) ?: return ""
        return formatWithoutUnit(amount)
    }

    fun formatInputOnChange(newValue: String): String {
        val digits = newValue.filter { it.isDigit() || it in '۰'..'۹' }
            .map { if (it in '۰'..'۹') '0' + (it - '۰') else it }
            .joinToString("")
        if (digits.isEmpty()) return ""
        val amount = digits.toLongOrNull() ?: return ""
        return formatWithoutUnit(amount)
    }

    /**
     * VisualTransformation that keeps the underlying value as pure digits (or empty)
     * and displays it with thousand separators + Persian digits.
     * This prevents cursor-jump / wrong-insertion bugs when commas appear.
     */
    class AmountVisualTransformation : VisualTransformation {
        override fun filter(text: AnnotatedString): TransformedText {
            val original = text.text
            val digitsOnly = original.filter { it.isDigit() }
            if (digitsOnly.isEmpty()) {
                return TransformedText(AnnotatedString(""), OffsetMapping.Identity)
            }
            val amount = digitsOnly.toLongOrNull() ?: 0L
            val formatted = formatWithoutUnit(amount)

            val offsetMapping = object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int {
                    var digitCount = 0
                    for (i in 0 until minOf(offset, original.length)) {
                        if (original[i].isDigit()) digitCount++
                    }
                    var transformedOffset = 0
                    var seenDigits = 0
                    while (transformedOffset < formatted.length && seenDigits < digitCount) {
                        if (formatted[transformedOffset].isDigit() || formatted[transformedOffset] in '۰'..'۹') {
                            seenDigits++
                        }
                        transformedOffset++
                    }
                    return transformedOffset.coerceIn(0, formatted.length)
                }

                override fun transformedToOriginal(offset: Int): Int {
                    var digitCount = 0
                    for (i in 0 until minOf(offset, formatted.length)) {
                        if (formatted[i].isDigit() || formatted[i] in '۰'..'۹') {
                            digitCount++
                        }
                    }
                    return digitCount.coerceIn(0, original.length)
                }
            }
            return TransformedText(AnnotatedString(formatted), offsetMapping)
        }
    }
}
