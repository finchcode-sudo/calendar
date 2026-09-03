package com.calendar.cc

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

    /**
     * 农历 1900-2100 的闰大小信息表（权威数据，来源于紫金山天文台算法的通用实现）
     * 每个数值的低 4 位表示当年闰几月（0 表示无闰月）；
     * 从第 5 位到第 17 位表示农历 13 个月的大小月分布（1 为大月30天，0 为小月29天）；
     * 第 17 位（0x10000）表示闰月是否为大月。
     */
    private val LUNAR_INFO = intArrayOf(
        0x04bd8, 0x04ae0, 0x0a570, 0x054d5, 0x0d260, 0x0d950, 0x16554, 0x056a0, 0x09ad0, 0x055d2, // 1900-1909
        0x04ae0, 0x0a5b6, 0x0a4d0, 0x0d250, 0x1d255, 0x0b540, 0x0d6a0, 0x0ada2, 0x095b0, 0x14977, // 1910-1919
        0x04970, 0x0a4b0, 0x0b4b5, 0x06a50, 0x06d40, 0x1ab54, 0x02b60, 0x09570, 0x052f2, 0x04970, // 1920-1929
        0x06566, 0x0d4a0, 0x0ea50, 0x06e95, 0x05ad0, 0x02b60, 0x186e3, 0x092e0, 0x1c8d7, 0x0c950, // 1930-1939
        0x0d4a0, 0x1d8a6, 0x0b550, 0x056a0, 0x1a5b4, 0x025d0, 0x092d0, 0x0d2b2, 0x0a950, 0x0b557, // 1940-1949
        0x06ca0, 0x0b550, 0x15355, 0x04da0, 0x0a5b0, 0x14573, 0x052b0, 0x0a9a8, 0x0e950, 0x06aa0, // 1950-1959
        0x0aea6, 0x0ab50, 0x04b60, 0x0aae4, 0x0a570, 0x05260, 0x0f263, 0x0d950, 0x05b57, 0x056a0, // 1960-1969
        0x096d0, 0x04dd5, 0x04ad0, 0x0a4d0, 0x0d4d4, 0x0d250, 0x0d558, 0x0b540, 0x0b6a0, 0x195a6, // 1970-1979
        0x095b0, 0x049b0, 0x0a974, 0x0a4b0, 0x0b27a, 0x06a50, 0x06d40, 0x0af46, 0x0ab60, 0x09570, // 1980-1989
        0x04af5, 0x04970, 0x064b0, 0x074a3, 0x0ea50, 0x06b58, 0x055c0, 0x0ab60, 0x096d5, 0x092e0, // 1990-1999
        0x0c960, 0x0d954, 0x0d4a0, 0x0da50, 0x07552, 0x056a0, 0x0abb7, 0x025d0, 0x092d0, 0x0cab5, // 2000-2009
        0x0a950, 0x0b4a0, 0x0baa4, 0x0ad50, 0x055d9, 0x04ba0, 0x0a5b0, 0x15176, 0x052b0, 0x0a930, // 2010-2019
        0x07954, 0x06aa0, 0x0ad50, 0x05b52, 0x04b60, 0x0a6e6, 0x0a4e0, 0x0d260, 0x0ea65, 0x0d530, // 2020-2029
        0x05aa0, 0x076a3, 0x096d0, 0x04afb, 0x04ad0, 0x0a4d0, 0x1d0b6, 0x0d250, 0x0d520, 0x0dd45, // 2030-2039
        0x0b5a0, 0x056d0, 0x055b2, 0x049b0, 0x0a577, 0x0a4b0, 0x0aa50, 0x1b255, 0x06d20, 0x0ada0, // 2040-2049
        0x14b63, 0x09370, 0x049f8, 0x04970, 0x064b0, 0x168a6, 0x0ea50, 0x06b20, 0x1a6c4, 0x0aae0, // 2050-2059
        0x0a2e0, 0x0d2e3, 0x0c960, 0x0d557, 0x0d4a0, 0x0da50, 0x05d55, 0x056a0, 0x0a6d0, 0x055d4, // 2060-2069
        0x052d0, 0x0a9b8, 0x0a950, 0x0b4a0, 0x0b6a6, 0x0ad50, 0x055a0, 0x0aba4, 0x0a5b0, 0x052b0, // 2070-2079
        0x0b273, 0x06930, 0x07337, 0x06aa0, 0x0ad50, 0x14b55, 0x04b60, 0x0a570, 0x054e4, 0x0d160, // 2080-2089
        0x0e968, 0x0d520, 0x0daa0, 0x16aa6, 0x056d0, 0x04ae0, 0x0a9d4, 0x0a2d0, 0x0d150, 0x0f252, // 2090-2099
        0x0d520 // 2100
    )

    /** 越界年份（表覆盖 1900-2100）安全裁剪到边界索引，避免数组越界崩溃 */
    private fun lunarInfoIdx(y: Int) = (y - 1900).coerceIn(0, LUNAR_INFO.size - 1)

    private fun lunarLeapMonth(y: Int): Int = LUNAR_INFO[lunarInfoIdx(y)] and 0xf

    private fun lunarLeapDays(y: Int): Int {
        return if (lunarLeapMonth(y) != 0) {
            if ((LUNAR_INFO[lunarInfoIdx(y)] and 0x10000) != 0) 30 else 29
        } else 0
    }

    private fun lunarMonthDays(y: Int, m: Int): Int {
        if (m > 12 || m < 1) return 29
        return if ((LUNAR_INFO[lunarInfoIdx(y)] and (0x10000 shr m)) != 0) 30 else 29
    }

    private fun lunarYearDays(y: Int): Int {
        var sum = 348
        var i = 0x8000
        while (i > 0x8) {
            if ((LUNAR_INFO[lunarInfoIdx(y)] and i) != 0) sum += 1
            i = i shr 1
        }
        return sum + lunarLeapDays(y)
    }

    private data class LunarYMD(val year: Int, val month: Int, val day: Int, val isLeap: Boolean)

    /** 阳历转农历年月日（1900-02-19 ~ 2100-12-31，基于权威闰月表逐日推算） */
    private fun solarToLunarYMD(year: Int, month: Int, day: Int): LunarYMD {
        val epoch = java.time.LocalDate.of(year, month, day)
        val base = java.time.LocalDate.of(1900, 1, 31)
        var offset = java.time.temporal.ChronoUnit.DAYS.between(base, epoch).toInt()

        var i = 1900
        var temp = 0
        while (i < 2101 && offset > 0) {
            temp = lunarYearDays(i)
            offset -= temp
            i++
        }
        if (offset < 0) {
            offset += temp
            i--
        }
        val lYear = i

        val leap = lunarLeapMonth(lYear)
        var isLeap = false
        i = 1
        while (i < 13 && offset > 0) {
            if (leap > 0 && i == (leap + 1) && !isLeap) {
                i--
                isLeap = true
                temp = lunarLeapDays(lYear)
            } else {
                temp = lunarMonthDays(lYear, i)
            }
            if (isLeap && i == (leap + 1)) {
                isLeap = false
            }
            offset -= temp
            i++
        }
        if (offset == 0 && leap > 0 && i == leap + 1) {
            if (isLeap) {
                isLeap = false
            } else {
                isLeap = true
                i--
            }
        }
        if (offset < 0) {
            offset += temp
            i--
        }
        val lMonth = i
        val lDay = offset + 1
        return LunarYMD(lYear, lMonth, lDay, isLeap)
    }

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

    // ============ 农历转换（权威闰月表算法） ============

    fun solarToLunar(year: Int, month: Int, day: Int): FullDateInfo {
        val ymd = solarToLunarYMD(year, month, day)
        val lunarYear = ymd.year
        val lunarMonth = ymd.month
        val isLeap = ymd.isLeap
        val lunarDay = ymd.day

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
                val lastDay = lunarMonthDays(lunarYear, 12)
                if (lunarDay == lastDay) list.add("除夕")
            }

            lunarFest[lunarMonth]?.get(lunarDay)?.let { list.add(it) }
        }

        // 母亲节：5月第二个周日
        if (solarMonth == 5) {
            val cal = Calendar.getInstance()
            cal.set(solarYear, 4, 1)
            val fw = cal.get(Calendar.DAY_OF_WEEK) // 1=周日…7=周六
            // 当月第一个周日的日号；fw==1 时取 1，否则 ((2-fw)%7+7)%7
            val firstSunday = if (fw == 1) 1 else ((2 - fw) % 7 + 7) % 7
            val secondSunday = firstSunday + 7
            if (solarDay == secondSunday) list.add("母亲节")
        }

        // 父亲节：6月第三个周日
        if (solarMonth == 6) {
            val cal = Calendar.getInstance()
            cal.set(solarYear, 5, 1)
            val fw = cal.get(Calendar.DAY_OF_WEEK)
            val firstSunday = if (fw == 1) 1 else ((2 - fw) % 7 + 7) % 7
            val thirdSunday = firstSunday + 14
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
