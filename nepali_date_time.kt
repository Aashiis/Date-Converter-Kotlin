package com.nepali.adbsdateconverter.nepali_utils

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue

data class NepaliDateTime(
    val year: Int,
    val month: Int = 1,
    val day: Int = 1,
    val hour: Int = 0,
    val minute: Int = 0,
    val second: Int = 0,
    val millisecond: Int = 0,
    val microsecond: Int = 0
) {
    init {
        require(year in 1970..2250) { "Supported year is 1970-2250" }
    }

    val totalDays: Int
        get() = nepaliYears[year]?.get(month) ?: 0

    val totalDaysInYear: Int
        get() = nepaliYears[year]?.first() ?: 0

    val weekday: Int
        @RequiresApi(Build.VERSION_CODES.O)
        get() = toDateTime().dayOfWeek.value % 7 + 1

    @RequiresApi(Build.VERSION_CODES.O)
    fun isAfter(other: NepaliDateTime): Boolean = toDateTime().isAfter(other.toDateTime())

    @RequiresApi(Build.VERSION_CODES.O)
    fun isBefore(other: NepaliDateTime): Boolean = toDateTime().isBefore(other.toDateTime())

    fun mergeTime(hour: Int, minute: Int, second: Int) =
        NepaliDateTime(year, month, day, hour, minute, second)

    @RequiresApi(Build.VERSION_CODES.O)
    fun toDateTime(): LocalDateTime {
        var englishYear = 1913
        var englishMonth = 1
        var englishDay = 1

        var difference = nepaliDateDifference(
            this,
            NepaliDateTime(1969, 9, 18)
        )

        while (difference >= if (isLeapYear(englishYear)) 366 else 365) {
            difference -= if (isLeapYear(englishYear)) 366 else 365
            englishYear++
        }

        val monthDays = if (isLeapYear(englishYear)) englishLeapMonths else englishMonths
        var i = 0
        while (difference >= monthDays[i]) {
            englishMonth++
            difference -= monthDays[i]
            i++
        }

        englishDay += difference

        return LocalDateTime.of(
            englishYear,
            englishMonth,
            englishDay,
            hour,
            minute,
            second,
            millisecond * 1000 + microsecond
        )
    }

    private fun nepaliDateDifference(date: NepaliDateTime, refDate: NepaliDateTime): Int {
        return (countTotalNepaliDays(date.year, date.month, date.day) -
                countTotalNepaliDays(refDate.year, refDate.month, refDate.day)).absoluteValue
    }

    private fun countTotalNepaliDays(year: Int, month: Int, day: Int): Int {
        var total = day - 1
        for (i in 1 until month) {
            total += nepaliYears[year]?.get(i) ?: 0
        }
        for (i in 1969 until year) {
            total += nepaliYears[i]?.first() ?: 0
        }
        return total
    }

    private fun isLeapYear(year: Int) = (year % 4 == 0) && ((year % 100 != 0) || (year % 400 == 0))

    override fun toString(): String {
        val y = fourDigits(year)
        val m = twoDigits(month)
        val d = twoDigits(day)
        val h = twoDigits(hour)
        val min = twoDigits(minute)
        val sec = twoDigits(second)
        val ms = threeDigits(millisecond)
        val us = if (microsecond == 0) "" else threeDigits(microsecond)
        return "$y-$m-$d $h:$min:$sec.$ms$us"
    }

    fun toIso8601String(): String {
        val y = if (year in -9999..9999) fourDigits(year) else sixDigits(year)
        val m = twoDigits(month)
        val d = twoDigits(day)
        val h = twoDigits(hour)
        val min = twoDigits(minute)
        val sec = twoDigits(second)
        val ms = threeDigits(millisecond)
        val us = if (microsecond == 0) "" else threeDigits(microsecond)
        return "$y-$m-$d$h:$min:$sec.$ms$us"
    }

    companion object {
        private val parseFormat = Regex(
            "^([+-]?\\d{4,6})-?(\\d\\d)-?(\\d\\d)" +
                    "(?:[ T](\\d\\d)(?::?(\\d\\d)(?::?(\\d\\d)(?:[.,](\\d{1,6}))?)?)?" +
                    "( ?[zZ]| ?([-+])(\\d\\d)(?::?(\\d\\d))?)?)?$"
        )

        fun parse(formattedString: String): NepaliDateTime {
            val match = parseFormat.matchEntire(formattedString)
                ?: throw IllegalArgumentException("Invalid NepaliDateTime format: $formattedString")

            val (years, month, day, hour, minute, second, milliAndMicroseconds) = match.destructured
            val millisecond = milliAndMicroseconds.toIntOrNull()?.let {
                it / 1000
            } ?: 0
            val microsecond = milliAndMicroseconds.toIntOrNull()?.let {
                it % 1000
            } ?: 0

            return NepaliDateTime(
                years.toInt(),
                month.toInt(),
                day.toInt(),
                hour.toIntOrNull() ?: 0,
                minute.toIntOrNull() ?: 0,
                second.toIntOrNull() ?: 0,
                millisecond,
                microsecond
            )
        }

        fun tryParse(formattedString: String): NepaliDateTime? {
            return try {
                parse(formattedString)
            } catch (e: IllegalArgumentException) {
                null
            }
        }

        private fun fourDigits(n: Int): String {
            val absN = n.absoluteValue
            val sign = if (n < 0) "-" else ""
            return when {
                absN >= 1000 -> "$n"
                absN >= 100 -> "${sign}0$absN"
                absN >= 10 -> "${sign}00$absN"
                else -> "${sign}000$absN"
            }
        }

        private fun sixDigits(n: Int): String {
            require(n in -9999..9999) { "abs($n) can't be >= 10000" }
            val absN = n.absoluteValue
            val sign = if (n < 0) "-" else "+"
            return if (absN >= 100000) "$sign$absN" else "$sign 0$absN"
        }

        private fun threeDigits(n: Int): String {
            return when {
                n >= 100 -> "$n"
                n >= 10 -> "0$n"
                else -> "00$n"
            }
        }

        private fun twoDigits(n: Int): String {
            return if (n >= 10) "$n" else "0$n"
        }

        private val englishMonths = listOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        private val englishLeapMonths = listOf(31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun LocalDateTime.toNepaliDateTime(): NepaliDateTime {
    val nepalTzOffset = Duration.ofHours(5).plusMinutes(45)
    val now = this.toInstant(ZoneOffset.UTC).plus(nepalTzOffset).atZone(ZoneOffset.UTC).toLocalDateTime()
    var nepaliYear = 1970
    var nepaliMonth = 1
    var nepaliDay = 1

    val date = LocalDateTime.of(now.year, now.month, now.dayOfMonth, 0, 0)
    var difference = ChronoUnit.DAYS.between(LocalDateTime.of(1913, 4, 13, 0, 0), date).toInt()

    println(":::$date:::")
//    if (date.offset == ZoneOffset.ofHoursMinutes(5, 45) && date.isAfter(LocalDateTime.of(1986, 1, 1, 0, 0))) {
//        difference += 1
//    }

    var daysInYear = nepaliYears[nepaliYear]?.first() ?: 0
    while (difference >= daysInYear) {
        nepaliYear += 1
        difference -= daysInYear
        daysInYear = nepaliYears[nepaliYear]?.first() ?: 0
    }

    var daysInMonth = nepaliYears[nepaliYear]?.get(nepaliMonth) ?: 0
    while (difference >= daysInMonth) {
        difference -= daysInMonth
        nepaliMonth += 1
        daysInMonth = nepaliYears[nepaliYear]?.get(nepaliMonth) ?: 0
    }

    nepaliDay += difference

    return NepaliDateTime(
        nepaliYear,
        nepaliMonth,
        nepaliDay,
        now.hour,
        now.minute,
        now.second,
        now.nano / 1000
    )
}

private val nepaliYears = mapOf(
    1969 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
    1970 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    1971 to listOf (365, 31, 31, 32, 31, 32, 30, 30, 29, 30, 29, 30, 30),
    1972 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    1973 to listOf (365, 30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
    1974 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    1975 to listOf (365, 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
    1976 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    1977 to listOf (365, 30, 32, 31, 32, 31, 31, 29, 30, 30, 29, 29, 31),
    1978 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    1979 to listOf (365, 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
    1980 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    1981 to listOf (365, 31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 29, 31),
    1982 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    1983 to listOf (365, 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
    1984 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    1985 to listOf (365, 31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30),
    1986 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    1987 to listOf (365, 31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30),
    1988 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    1989 to listOf (365, 31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
    1990 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    1991 to listOf (365, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30),
    1992 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
    1993 to listOf (365, 31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
    1994 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    1995 to listOf (365, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30),
    1996 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
    1997 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    1998 to listOf (365, 31, 31, 32, 31, 32, 30, 30, 29, 30, 29, 30, 30),
    1999 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    2000 to listOf (365, 30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
    2001 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2002 to listOf (365, 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
    2003 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    2004 to listOf (365, 30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
    2005 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2006 to listOf (365, 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
    2007 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    2008 to listOf (365, 31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 29, 31),
    2009 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2010 to listOf (365, 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
    2011 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    2012 to listOf (365, 31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30),
    2013 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2014 to listOf (365, 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
    2015 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    2016 to listOf (365, 31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30),
    2017 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2018 to listOf (365, 31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30),
    2019 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
    2020 to listOf (365, 31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
    2021 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2022 to listOf (365, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30),
    2023 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
    2024 to listOf (365, 31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
    2025 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2026 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    2027 to listOf (365, 30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
    2028 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2029 to listOf (365, 31, 31, 32, 31, 32, 30, 30, 29, 30, 29, 30, 30),
    2030 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    2031 to listOf (365, 30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
    2032 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2033 to listOf (365, 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
    2034 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    2035 to listOf (365, 30, 32, 31, 32, 31, 31, 29, 30, 30, 29, 29, 31),
    2036 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2037 to listOf (365, 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
    2038 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    2039 to listOf (365, 31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30),
    2040 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2041 to listOf (365, 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
    2042 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    2043 to listOf (365, 31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30),
    2044 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2045 to listOf (365, 31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30),
    2046 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    2047 to listOf (365, 31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
    2048 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2049 to listOf (365, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30),
    2050 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
    2051 to listOf (365, 31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
    2052 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2053 to listOf (365, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30),
    2054 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
    2055 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2056 to listOf (365, 31, 31, 32, 31, 32, 30, 30, 29, 30, 29, 30, 30),
    2057 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    2058 to listOf (365, 30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
    2059 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2060 to listOf (365, 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
    2061 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    2062 to listOf (365, 30, 32, 31, 32, 31, 31, 29, 30, 29, 30, 29, 31),
    2063 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2064 to listOf (365, 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
    2065 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    2066 to listOf (365, 31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 29, 31),
    2067 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2068 to listOf (365, 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
    2069 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    2070 to listOf (365, 31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30),
    2071 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2072 to listOf (365, 31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30),
    2073 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    2074 to listOf (365, 31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
    2075 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2076 to listOf (365, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30),
    2077 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
    2078 to listOf (365, 31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
    2079 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2080 to listOf (365, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30),
    2081 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
    2082 to listOf (365, 31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
    2083 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2084 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    2085 to listOf (365, 30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
    2086 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2087 to listOf (365, 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
    2088 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    2089 to listOf (365, 30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
    2090 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2091 to listOf (365, 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
    2092 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    2093 to listOf (365, 31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 29, 31),
    2094 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2095 to listOf (365, 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
    2096 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    2097 to listOf (365, 31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30),
    2098 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2099 to listOf (365, 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
    2100 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    2101 to listOf (365, 31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30),
    2102 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2103 to listOf (365, 31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30),
    2104 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
    2105 to listOf (365, 31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
    2106 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2107 to listOf (365, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30),
    2108 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
    2109 to listOf (365, 31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
    2110 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2111 to listOf (365, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30),
    2112 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
    2113 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2114 to listOf (365, 31, 31, 32, 31, 32, 30, 30, 29, 30, 29, 30, 30),
    2115 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    2116 to listOf (365, 30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
    2117 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2118 to listOf (365, 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
    2119 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    2120 to listOf (365, 30, 32, 31, 32, 31, 31, 29, 30, 30, 29, 29, 31),
    2121 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2122 to listOf (365, 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
    2123 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    2124 to listOf (365, 31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30),
    2125 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2126 to listOf (365, 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
    2127 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    2128 to listOf (365, 31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30),
    2129 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2130 to listOf (365, 31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30),
    2131 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    2132 to listOf (365, 31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
    2133 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2134 to listOf (365, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30),
    2135 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
    2136 to listOf (365, 31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
    2137 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2138 to listOf (365, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30),
    2139 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
    2140 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2141 to listOf (365, 31, 31, 32, 31, 32, 30, 30, 29, 30, 29, 30, 30),
    2142 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    2143 to listOf (365, 30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
    2144 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2145 to listOf (365, 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
    2146 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    2147 to listOf (365, 30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
    2148 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2149 to listOf (365, 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
    2150 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    2151 to listOf (365, 31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 29, 31),
    2152 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2153 to listOf (365, 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
    2154 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    2155 to listOf (365, 31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30),
    2156 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2157 to listOf (365, 31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30),
    2158 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    2159 to listOf (365, 31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
    2160 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2161 to listOf (365, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30),
    2162 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
    2163 to listOf (365, 31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
    2164 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2165 to listOf (365, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30),
    2166 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
    2167 to listOf (365, 31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
    2168 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2169 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    2170 to listOf (365, 30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
    2171 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2172 to listOf (365, 31, 31, 32, 31, 32, 30, 30, 29, 30, 29, 30, 30),
    2173 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    2174 to listOf (365, 30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
    2175 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2176 to listOf (365, 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
    2177 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    2178 to listOf (365, 30, 32, 31, 32, 31, 31, 29, 30, 30, 29, 29, 31),
    2179 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2180 to listOf (365, 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
    2181 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    2182 to listOf (365, 31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30),
    2183 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2184 to listOf (365, 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
    2185 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    2186 to listOf (365, 31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30),
    2187 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2188 to listOf (365, 31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30),
    2189 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    2190 to listOf (365, 31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
    2191 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2192 to listOf (365, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30),
    2193 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
    2194 to listOf (365, 31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
    2195 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2196 to listOf (365, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30),
    2197 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
    2198 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2199 to listOf (365, 31, 31, 32, 31, 32, 30, 30, 29, 30, 29, 30, 30),
    2200 to listOf (372, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31),
    2201 to listOf (365, 30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
    2202 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2203 to listOf (365, 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
    2204 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    2205 to listOf (365, 31, 31, 31, 32, 31, 31, 29, 30, 29, 30, 29, 31),
    2206 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2207 to listOf (365, 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
    2208 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    2209 to listOf (365, 31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30),
    2210 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2211 to listOf (365, 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
    2212 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    2213 to listOf (365, 31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30),
    2214 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2215 to listOf (365, 31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30),
    2216 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    2217 to listOf (365, 31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
    2218 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2219 to listOf (365, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30),
    2220 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
    2221 to listOf (365, 31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
    2222 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2223 to listOf (365, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30),
    2224 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
    2225 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2226 to listOf (365, 31, 31, 32, 31, 32, 30, 30, 29, 30, 29, 30, 30),
    2227 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    2228 to listOf (365, 30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
    2229 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2230 to listOf (365, 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
    2231 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    2232 to listOf (365, 30, 32, 31, 32, 31, 31, 29, 30, 29, 30, 29, 31),
    2233 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2234 to listOf (365, 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
    2235 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    2236 to listOf (365, 31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 29, 31),
    2237 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2238 to listOf (365, 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
    2239 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    2240 to listOf (365, 31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30),
    2241 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2242 to listOf (365, 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
    2243 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    2244 to listOf (365, 31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30),
    2245 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2246 to listOf (365, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30),
    2247 to listOf (366, 31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
    2248 to listOf (365, 31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
    2249 to listOf (365, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2250 to listOf (365, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30),
)
