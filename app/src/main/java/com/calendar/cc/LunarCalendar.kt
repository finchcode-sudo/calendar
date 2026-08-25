package com.calendar.cc

import android.icu.util.ChineseCalendar
import java.util.Calendar

object LunarCalendar {

    private val HEAVENLY_STEMS = arrayOf("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸")
    private val EARTHLY_BRANCHES = arrayOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")
    private val ZODIAC = arrayOf("鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊", "猴", "鸡", "狗", "猪")
    private val LUNAR_MONTH_NAMES = arrayOf("正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊")
    private val LUNAR_DAY_NAMES = arrayOf(
        "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
        "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
        "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
    )

    private val SOLAR_TERM_NAMES = arrayOf(
        "小寒", "大寒", "立春", "雨水", "惊蛰", "春分",
        "清明", "谷雨", "立夏", "小满", "芒种", "夏至",
        "小暑", "大暑", "立秋", "处暑", "白露", "秋分",
        "寒露", "霜降", "立冬", "小雪", "大雪", "冬至"
    )

    /** 1900-2000 年节气 C 值 */
    private val TERM_C_20 = doubleArrayOf(
        6.11, 20.84, 4.6295, 19.4599, 6.3826, 21.4155,
        5.59, 20.888, 6.318, 21.86, 6.5, 22.2,
        7.928, 23.65, 8.35, 23.95, 8.44, 23.822,
        9.098, 24.218, 8.218, 23.08, 7.9, 22.6
    )

    /** 2000-2100 年节气 C 值 */
    private val TERM_C_21 = doubleArrayOf(
        5.4055, 20.12, 3.87, 18.73, 5.63, 20.646,
        4.81, 20.1, 5.52, 21.04, 5.678, 21.37,
        7.108, 22.83, 7.5, 23.13, 7.646, 23.042,
        8.318, 23.438, 7.438, 22.36, 7.18, 21.94
    )

    data class DayInfo(
        val day: Int,
        val month: Int,
        val year: Int,
        val isCurrentMonth: Boolean,
        val isToday: Boolean,
        val lunarDay: String,
        val festivals: List<String> = emptyList(),
        val solarTerm: String? = null
    )

    data class FullDateInfo(
        val solarYear: Int,
        val solarMonth: Int,
        val solarDay: Int,
        val lunarYear: Int,
        val lunarMonth: Int,
        val lunarDay: Int,
        val isLeapMonth: Boolean,
        val yearName: String,
        val zodiac: String,
        val lunarMonthName: String,
        val lunarDayName: String,
        val solarTerms: List<String>,
        val festivals: List<String>
    )

    // ============ 农历转换（ICU） ============

    fun solarToLunar(year: Int, month: Int, day: Int): FullDateInfo {
        val cc = ChineseCalendar()
        cc.set(Calendar.YEAR, year)
        cc.set(Calendar.MONTH, month - 1)
        cc.set(Calendar.DAY_OF_MONTH, day)

        val lunarYear = cc.get(ChineseCalendar.EXTENDED_YEAR) - 2637
        val lunarMonth = cc.get(ChineseCalendar.MONTH) + 1
        val isLeap = cc.get(ChineseCalendar.IS_LEAP_MONTH) == 1
        val lunarDay = cc.get(ChineseCalendar.DAY_OF_MONTH)

        val ganZhiIdx = (lunarYear - 4) % 60
        val yearName = "${HEAVENLY_STEMS[ganZhiIdx % 10]}${EARTHLY_BRANCHES[ganZhiIdx % 12]}"
        val zodiac = ZODIAC[(lunarYear - 4) % 12]

        val lunarMonthName = if (isLeap) "闰${LUNAR_MONTH_NAMES[lunarMonth - 1]}月" else "${LUNAR_MONTH_NAMES[lunarMonth - 1]}月"
        val lunarDayName = if (lunarDay in 1..30) LUNAR_DAY_NAMES[lunarDay - 1] else "${lunarDay}日"

        val term = getSolarTerm(year, month, day)

        return FullDateInfo(
            solarYear = year, solarMonth = month, solarDay = day,
            lunarYear = lunarYear, lunarMonth = lunarMonth, lunarDay = lunarDay,
            isLeapMonth = isLeap,
            yearName = yearName, zodiac = zodiac,
            lunarMonthName = lunarMonthName, lunarDayName = lunarDayName,
            solarTerms = if (term != null) listOf(term) else emptyList(),
            festivals = getFestivals(year, month, day, lunarYear, lunarMonth, lunarDay, isLeap, term)
        )
    }

    // ============ 节气（寿星公式） ============

    private fun getSolarTerm(year: Int, month: Int, day: Int): String? {
        val cValues = if (year in 1900..1999) TERM_C_20 else TERM_C_21
        val y = year % 100
        val d = 0.2422
        val l = if (year in 1900..1999) y / 4 + 1 else y / 4 

        // 每月两个节气
        for (offset in 0..1) {
            val idx = (month - 1) * 2 + offset
            if (idx > 23) break
            val termDay = (y * d + cValues[idx] - l).toInt()
            if (termDay == day) return SOLAR_TERM_NAMES[idx]
        }

        return null
    }

    // ============ 节日 ============

    private fun getFestivals(
        solarYear: Int, solarMonth: Int, solarDay: Int,
        lunarYear: Int, lunarMonth: Int, lunarDay: Int, isLeap: Boolean,
        solarTerm: String?
    ): List<String> {
        val list = mutableListOf<String>()

        // 公历固定节日
        val solarFest = mapOf(
            1 to mapOf(1 to "元旦"),
            2 to mapOf(14 to "情人节"),
            3 to mapOf(8 to "妇女节", 12 to "植树节"),
            4 to mapOf(1 to "愚人节"),
            5 to mapOf(1 to "劳动节", 4 to "青年节", 12 to "护士节"),
            6 to mapOf(1 to "儿童节"),
            7 to mapOf(1 to "建党节"),
            8 to mapOf(1 to "建军节"),
            9 to mapOf(10 to "教师节"),
            10 to mapOf(1 to "国庆节", 31 to "万圣节"),
            11 to mapOf(11 to "光棍节"),
            12 to mapOf(25 to "圣诞节")
        )
        solarFest[solarMonth]?.get(solarDay)?.let { list.add(it) }

        // 农历节日
        if (!isLeap) {
            val lunarFest = mapOf(
                1 to mapOf(1 to "春节", 15 to "元宵节"),
                2 to mapOf(2 to "龙抬头"),
                5 to mapOf(5 to "端午节"),
                7 to mapOf(7 to "七夕节", 15 to "中元节"),
                8 to mapOf(15 to "中秋节"),
                9 to mapOf(9 to "重阳节"),
                12 to mapOf(8 to "腊八节", 23 to "小年")
            )

            // 除夕：腊月最后一天
            if (lunarMonth == 12) {
                val cc = ChineseCalendar()
                cc.set(ChineseCalendar.EXTENDED_YEAR, lunarYear + 2637)
                cc.set(ChineseCalendar.MONTH, 11)
                cc.set(ChineseCalendar.DAY_OF_MONTH, 1)
                val lastDay = cc.getActualMaximum(ChineseCalendar.DAY_OF_MONTH)
                if (lunarDay == lastDay) list.add("除夕")
            }

            lunarFest[lunarMonth]?.get(lunarDay)?.let { list.add(it) }
        }

        // 母亲节：5月第二个周日
        if (solarMonth == 5) {
            val cal = Calendar.getInstance()
            cal.set(solarYear, 4, 1)
            val fw = cal.get(Calendar.DAY_OF_WEEK)
            val secondSunday = 1 + (if (fw == 1) 7 else (7 - fw + 1) % 7) + 7
            if (solarDay == secondSunday) list.add("母亲节")
        }

        // 父亲节：6月第三个周日
        if (solarMonth == 6) {
            val cal = Calendar.getInstance()
            cal.set(solarYear, 5, 1)
            val fw = cal.get(Calendar.DAY_OF_WEEK)
            val thirdSunday = 1 + (if (fw == 1) 7 else (7 - fw + 1) % 7) + 14
            if (solarDay == thirdSunday) list.add("父亲节")
        }

        return list
    }

    // ============ 日历网格 ============

    fun getMonthDays(year: Int, month: Int): List<DayInfo> {
        val result = mutableListOf<DayInfo>()

        val cal = Calendar.getInstance().apply {
            set(year, month - 1, 1)
        }
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=周日, 2=周一...
        val startOffset = firstDayOfWeek - 1

        // 上月尾巴
        if (startOffset > 0) {
            val prev = (cal.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
            val py = prev.get(Calendar.YEAR)
            val pm = prev.get(Calendar.MONTH) + 1
            val pmd = prev.getActualMaximum(Calendar.DAY_OF_MONTH)
            for (i in (pmd - startOffset + 1)..pmd) {
                val info = solarToLunar(py, pm, i)
                result.add(
                    DayInfo(i, pm, py, false, false, info.lunarDayName,
                        info.festivals, getSolarTerm(py, pm, i))
                )
            }
        }

        // 当月
        val today = Calendar.getInstance()
        val ty = today.get(Calendar.YEAR)
        val tm = today.get(Calendar.MONTH) + 1
        val td = today.get(Calendar.DAY_OF_MONTH)

        for (d in 1..daysInMonth) {
            val info = solarToLunar(year, month, d)
            val isToday = (year == ty && month == tm && d == td)
            result.add(
                DayInfo(d, month, year, true, isToday, info.lunarDayName,
                    info.festivals, getSolarTerm(year, month, d))
            )
        }

        // 下月开头
        val remaining = 42 - result.size
        if (remaining > 0) {
            val next = (cal.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
            val ny = next.get(Calendar.YEAR)
            val nm = next.get(Calendar.MONTH) + 1
            for (i in 1..remaining) {
                val info = solarToLunar(ny, nm, i)
                result.add(
                    DayInfo(i, nm, ny, false, false, info.lunarDayName,
                        info.festivals, getSolarTerm(ny, nm, i))
                )
            }
        }

        return result
    }
}
