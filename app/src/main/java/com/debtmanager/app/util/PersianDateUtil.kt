package com.debtmanager.app.util

import java.util.Calendar

/**
 * Lightweight Jalali (Persian) calendar utilities.
 */
object PersianDateUtil {

    private val gregorianDaysInMonth = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    private val jalaliDaysInMonth = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)

    data class JalaliDate(val year: Int, val month: Int, val day: Int)

    fun fromTimestamp(millis: Long): JalaliDate {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return gregorianToJalali(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
    }

    fun toTimestamp(jy: Int, jm: Int, jd: Int): Long {
        val (gy, gm, gd) = jalaliToGregorian(jy, jm, jd)
        return Calendar.getInstance().apply {
            set(gy, gm - 1, gd, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun format(millis: Long): String {
        val j = fromTimestamp(millis)
        return "${toPersianDigits(j.day)} ${monthName(j.month)} ${toPersianDigits(j.year)}"
    }

    fun formatShort(millis: Long): String {
        val j = fromTimestamp(millis)
        return "${toPersianDigits(j.year)}/${toPersianDigits(String.format("%02d", j.month))}/${toPersianDigits(String.format("%02d", j.day))}"
    }

    fun monthName(month: Int): String = when (month) {
        1 -> "فروردین"; 2 -> "اردیبهشت"; 3 -> "خرداد"; 4 -> "تیر"
        5 -> "مرداد"; 6 -> "شهریور"; 7 -> "مهر"; 8 -> "آبان"
        9 -> "آذر"; 10 -> "دی"; 11 -> "بهمن"; 12 -> "اسفند"
        else -> ""
    }

    fun toPersianDigits(input: Any): String {
        val persian = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        return input.toString().map { c ->
            if (c in '0'..'9') persian[c - '0'] else c
        }.joinToString("")
    }

    fun startOfDay(millis: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun endOfDay(millis: Long): Long = startOfDay(millis) + 86_399_999L

    fun startOfMonth(millis: Long): Long {
        val j = fromTimestamp(millis)
        return toTimestamp(j.year, j.month, 1)
    }

    fun endOfMonth(millis: Long): Long {
        val j = fromTimestamp(millis)
        val days = if (j.month <= 6) 31 else if (j.month <= 11) 30 else if (isLeapYear(j.year)) 30 else 29
        return endOfDay(toTimestamp(j.year, j.month, days))
    }

    fun addMonths(millis: Long, months: Int): Long {
        val j = fromTimestamp(millis)
        var y = j.year
        var m = j.month + months
        while (m > 12) { m -= 12; y++ }
        while (m < 1) { m += 12; y-- }
        val maxDay = if (m <= 6) 31 else if (m <= 11) 30 else if (isLeapYear(y)) 30 else 29
        val d = j.day.coerceAtMost(maxDay)
        return toTimestamp(y, m, d)
    }

    fun addDays(millis: Long, days: Int): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        cal.add(Calendar.DAY_OF_MONTH, days)
        return cal.timeInMillis
    }

    fun daysBetween(from: Long, to: Long): Int {
        val diff = startOfDay(to) - startOfDay(from)
        return (diff / 86_400_000L).toInt()
    }

    fun isLeapYear(jy: Int): Boolean {
        val breaks = intArrayOf(-61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210, 1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178)
        var jp = breaks[0]
        var jump: Int
        var leapJ = -14
        var n = 1
        var i = 1
        while (i < breaks.size) {
            val jm = breaks[i]
            jump = jm - jp
            if (jy < jm) break
            leapJ += jump / 33 * 8 + (jump % 33) / 4
            jp = jm
            i++
            n++
        }
        var leap = ((jy - jp) / 33 * 8 + ((jy - jp) % 33 + 3) / 4)
        if ((jump % 33) == 4 && jump - (jy - jp) == 4) leap++
        return (((jy + 1) % 33 - 1) % 4) == 0
    }

    private fun gregorianToJalali(gy: Int, gm: Int, gd: Int): JalaliDate {
        var gYear = gy
        var gDayNo = 365 * (gYear - 1600) + (gYear - 1601) / 4 - (gYear - 1601) / 100 + (gYear - 1601) / 400
        for (i in 0 until gm - 1) gDayNo += gregorianDaysInMonth[i]
        if (gm > 2 && isGregorianLeap(gYear)) gDayNo++
        gDayNo += gd - 1

        var jDayNo = gDayNo - 79
        val jNp = jDayNo / 12053
        jDayNo %= 12053
        var jy = 979 + 33 * jNp + 4 * (jDayNo / 1461)
        jDayNo %= 1461
        if (jDayNo >= 366) {
            jy += (jDayNo - 1) / 365
            jDayNo = (jDayNo - 1) % 365
        }
        var jm: Int
        var i = 0
        while (i < 11 && jDayNo >= jalaliDaysInMonth[i]) {
            jDayNo -= jalaliDaysInMonth[i]
            i++
        }
        jm = i + 1
        val jd = jDayNo + 1
        return JalaliDate(jy, jm, jd)
    }

    private fun jalaliToGregorian(jy: Int, jm: Int, jd: Int): Triple<Int, Int, Int> {
        var jYear = jy
        var jDayNo = 365 * (jYear - 979) + (jYear - 979) / 33 * 8 + ((jYear - 979) % 33 + 3) / 4
        for (i in 0 until jm - 1) jDayNo += jalaliDaysInMonth[i]
        jDayNo += jd - 1
        var gDayNo = jDayNo + 79
        var gy = 1600 + 400 * (gDayNo / 146097)
        gDayNo %= 146097
        var leap = true
        if (gDayNo >= 36525) {
            gDayNo--
            gy += 100 * (gDayNo / 36524)
            gDayNo %= 36524
            if (gDayNo >= 365) gDayNo++ else leap = false
        }
        gy += 4 * (gDayNo / 1461)
        gDayNo %= 1461
        if (gDayNo >= 366) {
            leap = false
            gDayNo--
            gy += gDayNo / 365
            gDayNo %= 365
        }
        var gm = 0
        val days = if (leap) intArrayOf(31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        else intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        while (gm < 12 && gDayNo >= days[gm]) {
            gDayNo -= days[gm]
            gm++
        }
        return Triple(gy, gm + 1, gDayNo + 1)
    }

    private fun isGregorianLeap(year: Int) = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
}
