package com.calendar.cc

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calendar.cc.LunarCalendar.DayInfo
import com.calendar.cc.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen() {
    val calendar = remember { Calendar.getInstance() }

    // 月份分页范围：1900年1月 ~ 2100年12月，用于支持左右滑动切换月份
    val minYear = 1900
    val maxYear = 2100
    val totalPages = remember { (maxYear - minYear + 1) * 12 }
    fun yearMonthToPage(year: Int, month: Int) = (year - minYear) * 12 + (month - 1)
    fun pageToYear(page: Int) = minYear + page / 12
    fun pageToMonth(page: Int) = page % 12 + 1

    val pagerState = rememberPagerState(
        initialPage = yearMonthToPage(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1),
        pageCount = { totalPages }
    )
    val pagerScope = rememberCoroutineScope()

    // 记录上一个稳定页，用于判断是否是"滑动/翻页"触发的月份变化
    var lastSettledPage by remember { mutableIntStateOf(pagerState.currentPage) }

    var selectedDay by remember { mutableIntStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }
    var showYearPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showAddEvent by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<CalendarEvent?>(null) }

    var refreshTrigger by remember { mutableIntStateOf(0) }

    val currentYear = pageToYear(pagerState.currentPage)
    val currentMonth = pageToMonth(pagerState.currentPage)

    // 左右滑动（或翻页按钮）切换到新月份时，选中日重置为1号
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != lastSettledPage) {
            selectedDay = 1
            lastSettledPage = pagerState.currentPage
        }
    }

    val selectedInfo = remember(currentYear, currentMonth, selectedDay, refreshTrigger) {
        LunarCalendar.solarToLunar(currentYear, currentMonth, selectedDay)
    }

    val eventsForSelected = remember(currentYear, currentMonth, selectedDay, refreshTrigger) {
        EventManager.getEventsForDate(currentYear, currentMonth, selectedDay)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // === 顶部标题栏 ===
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${currentYear}年",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Row {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(
                        Icons.Filled.DateRange,
                        contentDescription = "跳转日期",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { showYearPicker = true }) {
                    Icon(
                        Icons.Filled.CalendarMonth,
                        contentDescription = "选择年份",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

            // === 月份切换栏 ===
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = {
                    pagerScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                }) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "上月", tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                    Text("上月", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                }

                Text(
                    text = "${currentMonth}月",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold, fontSize = 28.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.animateContentSize()
                )

                TextButton(onClick = {
                    pagerScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }) {
                    Text("下月", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Filled.ChevronRight, contentDescription = "下月", tint = MaterialTheme.colorScheme.primary)
                }
            }

            // === 干支纪年 & 生肖 ===
            val yearGanZhi = remember(currentYear, currentMonth) {
                LunarCalendar.solarToLunar(currentYear, currentMonth, 1).yearName
            }
            val zodiac = remember(currentYear, currentMonth) {
                LunarCalendar.solarToLunar(currentYear, currentMonth, 1).zodiac
            }
            Text(
                text = "${yearGanZhi}年 · 生肖${zodiac}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )

            // === 星期头部 ===
            val weekDays = arrayOf("日", "一", "二", "三", "四", "五", "六")
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                weekDays.forEachIndexed { index, day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium, fontSize = 13.sp
                        ),
                        color = if (index == 0 || index == 6)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // === 日期网格（支持左右滑动切换月份） ===
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                val pageYear = pageToYear(page)
                val pageMonth = pageToMonth(page)
                val pageDays = remember(pageYear, pageMonth, refreshTrigger) {
                    LunarCalendar.getMonthDays(pageYear, pageMonth)
                }
                val pageEventsForMonth = remember(pageYear, pageMonth, refreshTrigger) {
                    EventManager.getEventsForMonth(pageYear, pageMonth)
                }
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                ) {
                    pageDays.chunked(7).forEach { week ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            week.forEachIndexed { index, dayInfo ->
                                Box(modifier = Modifier.weight(1f)) {
                                    if (dayInfo.isCurrentMonth) {
                                        DayCell(
                                            dayInfo = dayInfo,
                                            isSunday = index == 0,
                                            isSaturday = index == 6,
                                            isSelected = page == pagerState.currentPage && dayInfo.day == selectedDay,
                                            hasEvent = pageEventsForMonth.any { it.day == dayInfo.day },
                                            onClick = {
                                                selectedDay = dayInfo.day
                                                if (page != pagerState.currentPage) {
                                                    lastSettledPage = page
                                                    pagerScope.launch { pagerState.animateScrollToPage(page) }
                                                }
                                            }
                                        )
                                    } else {
                                        // 非本月日期留空
                                        Spacer(Modifier.aspectRatio(0.85f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // === 底部操作栏 ===
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(
                    onClick = {
                        val now = Calendar.getInstance()
                        val todayPage = yearMonthToPage(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1)
                        selectedDay = now.get(Calendar.DAY_OF_MONTH)
                        lastSettledPage = todayPage
                        pagerScope.launch { pagerState.animateScrollToPage(todayPage) }
                    },
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    )
                ) {
                    Icon(Icons.Filled.Today, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(6.dp))
                    Text("今天", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                }

                Button(
                    onClick = { showAddEvent = true },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("日程", style = MaterialTheme.typography.labelLarge)
                }
            }

            // === 选中日期详情卡片 ===
            DateDetailCard(
                selectedInfo,
                eventsForSelected,
                onAddEvent = { editingEvent = null; showAddEvent = true },
                onDeleteEvent = { refreshTrigger++ },
                onEditEvent = { event -> editingEvent = event; showAddEvent = true }
            )

            Spacer(Modifier.height(16.dp))
        }

    // 年份选择弹窗
    if (showYearPicker) {
        YearPickerDialog(
            currentYear = currentYear,
            onYearSelected = { year ->
                val targetPage = yearMonthToPage(year, currentMonth)
                lastSettledPage = targetPage
                pagerScope.launch { pagerState.scrollToPage(targetPage) }
                showYearPicker = false
            },
            onDismiss = { showYearPicker = false }
        )
    }

    // 跳转日期弹窗
    if (showDatePicker) {
        DateJumpDialog(
            onDateSelected = { year, month, day ->
                val targetPage = yearMonthToPage(year, month)
                lastSettledPage = targetPage
                selectedDay = day
                pagerScope.launch { pagerState.scrollToPage(targetPage) }
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    // 添加/编辑日程弹窗
    if (showAddEvent) {
        AddEventDialog(
            year = currentYear,
            month = currentMonth,
            day = selectedDay,
            existingEvent = editingEvent,
            onDismiss = { showAddEvent = false; editingEvent = null },
            onSaved = {
                showAddEvent = false
                editingEvent = null
                refreshTrigger++
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DayCell(
    dayInfo: DayInfo,
    isSunday: Boolean,
    isSaturday: Boolean,
    isSelected: Boolean,
    hasEvent: Boolean,
    onClick: () -> Unit
) {
    val bgColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        dayInfo.isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        else -> Color.Transparent
    }

    val textColor = when {
        isSelected -> Color.White
        dayInfo.isToday -> MaterialTheme.colorScheme.primary
        isSunday || isSaturday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    val hasFestival = dayInfo.festivals.any { it in listOf("春节", "元旦", "元宵节", "龙抬头", "端午节", "七夕节", "中元节", "中秋节", "重阳节", "腊八节", "小年", "除夕", "劳动节", "国庆节", "情人节", "妇女节", "植树节", "愚人节", "青年节", "护士节", "儿童节", "建党节", "建军节", "教师节", "万圣节", "光棍节", "圣诞节", "母亲节", "父亲节") }
    val hasSolarTerm = dayInfo.solarTerm != null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .then(
                if (dayInfo.isToday && !isSelected)
                    Modifier.border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    )
                else Modifier
            )
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 公历日
        Text(
            text = "${dayInfo.day}",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (isSelected || dayInfo.isToday) FontWeight.Bold else FontWeight.Normal,
                fontSize = if (isSelected) 20.sp else 16.sp
            ),
            color = textColor
        )

        // 农历日或节气
        val subText = if (hasSolarTerm) {
            dayInfo.solarTerm.let { if (it.length <= 2) it else it.take(2) }
        } else if (hasFestival) {
            val fest = dayInfo.festivals.firstOrNull {
                it in listOf("春节", "元旦", "元宵节", "龙抬头", "端午节", "七夕节", "中元节", "中秋节", "重阳节", "腊八节", "小年", "除夕", "劳动节", "国庆节", "情人节", "妇女节", "植树节", "愚人节", "青年节", "护士节", "儿童节", "建党节", "建军节", "教师节", "万圣节", "光棍节", "圣诞节", "母亲节", "父亲节")
            }
            when (fest) {
                "春节" -> "春节"
                "元旦" -> "元旦"
                "国庆节" -> "国庆节"
                "端午节" -> "端午节"
                "中秋节" -> "中秋节"
                "中元节" -> "中元节"
                "元宵节" -> "元宵节"
                "劳动节" -> "劳动节"
                "七夕节" -> "七夕节"
                "重阳节" -> "重阳节"
                "腊八节" -> "腊八节"
                "小年" -> "小年"
                "除夕" -> "除夕"
                "龙抬头", "情人节", "妇女节", "植树节", "愚人节", "青年节", "护士节", "儿童节", "建党节", "建军节", "教师节", "万圣节", "光棍节", "圣诞节", "母亲节", "父亲节" -> fest
                else -> ""
            }
        } else {
            dayInfo.lunarDay
        }

        Text(
            text = subText,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = if (hasFestival || hasSolarTerm) FontWeight.Bold else FontWeight.Normal
            ),
            color = when {
                isSelected -> Color.White.copy(alpha = 0.85f)
                hasFestival -> FestivalRed
                hasSolarTerm -> Sage
                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // 日程圆点标记
        if (hasEvent) {
            Spacer(Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Color.White else MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
private fun DateDetailCard(
    info: LunarCalendar.FullDateInfo,
    events: List<CalendarEvent>,
    onAddEvent: () -> Unit,
    onDeleteEvent: () -> Unit = {},
    onEditEvent: (CalendarEvent) -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp)
        ) {
            // 第一行：公历日期 + 添加按钮
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${info.solarMonth}月${info.solarDay}日",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold, fontSize = 24.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = getWeekdayName(info.solarYear, info.solarMonth, info.solarDay),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }
                IconButton(onClick = onAddEvent) {
                    Icon(
                        Icons.Filled.AddCircle,
                        contentDescription = "添加日程",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 1.dp)
            Spacer(Modifier.height(8.dp))

            // 农历信息
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.NightsStay, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    "${info.yearName}年 【${info.zodiac}年】",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.WbSunny, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.tertiary)
                Spacer(Modifier.width(8.dp))
                Text(
                    "农历${info.lunarMonthName}${info.lunarDayName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // 节日 & 节气
            Spacer(Modifier.height(8.dp))
            if (info.festivals.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    info.festivals.forEach { festival ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = FestivalRed.copy(alpha = 0.12f)
                        ) {
                            Text(
                                festival,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                                color = FestivalRed,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
            if (info.solarTerms.isNotEmpty()) {
                if (info.festivals.isNotEmpty()) Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    info.solarTerms.forEach { term ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Sage.copy(alpha = 0.12f)
                        ) {
                            Text(
                                term,
                                style = MaterialTheme.typography.labelMedium,
                                color = Sage,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // 日程列表
            if (events.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 1.dp)
                Spacer(Modifier.height(8.dp))

                Text(
                    "日程",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))

                events.forEach { event ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onEditEvent(event) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(event.color))
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                event.title,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                            )
                            if (event.hour >= 0) {
                                Text(
                                    "${event.hour.toString().padStart(2, '0')}:${event.minute.toString().padStart(2, '0')}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    "全天",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                EventManager.deleteEvent(event.id)
                                onDeleteEvent()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = "删除",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DateJumpDialog(
    onDateSelected: (year: Int, month: Int, day: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val calendar = Calendar.getInstance()
    var year by remember { mutableIntStateOf(calendar.get(Calendar.YEAR)) }
    var month by remember { mutableIntStateOf(calendar.get(Calendar.MONTH) + 1) }
    var day by remember { mutableIntStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text("跳转日期", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column {
                // 年份选择
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("年", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.width(32.dp))
                    Spacer(Modifier.width(4.dp))
                    TextButton(
                        onClick = { year = (year - 10).coerceIn(1900, 2100) },
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Text("-10", style = MaterialTheme.typography.labelLarge)
                    }
                    IconButton(onClick = { year = (year - 1).coerceIn(1900, 2100) }) {
                        Icon(Icons.Filled.Remove, contentDescription = null)
                    }
                    Text(
                        "$year",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.width(64.dp),
                        textAlign = TextAlign.Center
                    )
                    IconButton(onClick = { year = (year + 1).coerceIn(1900, 2100) }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                    }
                    TextButton(
                        onClick = { year = (year + 10).coerceIn(1900, 2100) },
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Text("+10", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Spacer(Modifier.height(8.dp))
                // 月份选择
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("月", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.width(32.dp))
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { month = if (month > 1) month - 1 else 12 }) {
                        Icon(Icons.Filled.Remove, contentDescription = null)
                    }
                    Text(
                        "$month",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.width(80.dp),
                        textAlign = TextAlign.Center
                    )
                    IconButton(onClick = { month = if (month < 12) month + 1 else 1 }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                    }
                }
                Spacer(Modifier.height(8.dp))
                // 日选择
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("日", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.width(32.dp))
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = {
                        if (day > 1) day--
                        else {
                            val cal = Calendar.getInstance()
                            cal.set(year, month - 1, 1)
                            cal.add(Calendar.DAY_OF_MONTH, -1)
                            day = cal.get(Calendar.DAY_OF_MONTH)
                        }
                    }) {
                        Icon(Icons.Filled.Remove, contentDescription = null)
                    }
                    Text(
                        "$day",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.width(80.dp),
                        textAlign = TextAlign.Center
                    )
                    IconButton(onClick = {
                        val cal = Calendar.getInstance()
                        cal.set(year, month - 1, 1)
                        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                        if (day < maxDay) day++
                        else day = 1
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onDateSelected(year, month, day) }) {
                Text("跳转")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventDialog(
    year: Int,
    month: Int,
    day: Int,
    existingEvent: CalendarEvent? = null,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    var title by remember { mutableStateOf(existingEvent?.title ?: "") }
    var isAllDay by remember { mutableStateOf(existingEvent?.let { it.hour < 0 } ?: true) }
    var hour by remember { mutableIntStateOf(existingEvent?.hour?.takeIf { it >= 0 } ?: 9) }
    var minute by remember { mutableIntStateOf(existingEvent?.minute ?: 0) }
    var note by remember { mutableStateOf(existingEvent?.note ?: "") }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(if (existingEvent != null) "编辑日程" else "添加日程", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column {
                // 日期显示
                Text(
                    "${year}年${month}月${day}日 ${getWeekdayName(year, month, day)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; showError = false },
                    label = { Text("日程标题") },
                    placeholder = { Text("输入日程标题…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = showError,
                    supportingText = if (showError) {{ Text("标题不能为空") }} else null,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(12.dp))

                // 全天开关
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isAllDay,
                        onCheckedChange = { isAllDay = it }
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("全天事件")
                }

                // 时间选择
                if (!isAllDay) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("时间：", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.width(8.dp))
                        // 小时
                        var showHourPicker by remember { mutableStateOf(false) }
                        TextButton(onClick = { showHourPicker = true }) {
                            Text("${hour.toString().padStart(2, '0')}", style = MaterialTheme.typography.titleMedium)
                        }
                        if (showHourPicker) {
                            AlertDialog(
                                onDismissRequest = { showHourPicker = false },
                                title = { Text("选择小时") },
                                text = {
                                    val hours = (0..23).toList()
                                    Column(modifier = Modifier.height(200.dp).verticalScroll(rememberScrollState())) {
                                        hours.forEach { h ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth().clickable { hour = h; showHourPicker = false }.padding(vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(selected = hour == h, onClick = { hour = h; showHourPicker = false })
                                                Spacer(Modifier.width(8.dp))
                                                Text("${h.toString().padStart(2, '0')} 时")
                                            }
                                        }
                                    }
                                },
                                confirmButton = { TextButton(onClick = { showHourPicker = false }) { Text("取消") } }
                            )
                        }
                        Text(":", style = MaterialTheme.typography.titleMedium)
                        var showMinPicker by remember { mutableStateOf(false) }
                        TextButton(onClick = { showMinPicker = true }) {
                            Text("${minute.toString().padStart(2, '0')}", style = MaterialTheme.typography.titleMedium)
                        }
                        if (showMinPicker) {
                            AlertDialog(
                                onDismissRequest = { showMinPicker = false },
                                title = { Text("选择分钟") },
                                text = {
                                    val minutes = listOf(0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55)
                                    Column(modifier = Modifier.height(200.dp).verticalScroll(rememberScrollState())) {
                                        minutes.forEach { m ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth().clickable { minute = m; showMinPicker = false }.padding(vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(selected = minute == m, onClick = { minute = m; showMinPicker = false })
                                                Spacer(Modifier.width(8.dp))
                                                Text("${m.toString().padStart(2, '0')} 分")
                                            }
                                        }
                                    }
                                },
                                confirmButton = { TextButton(onClick = { showMinPicker = false }) { Text("取消") } }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注（可选）") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        showError = true
                        return@Button
                    }
                    val event = CalendarEvent(
                        id = existingEvent?.id ?: java.util.UUID.randomUUID().toString(),
                        title = title,
                        year = year,
                        month = month,
                        day = day,
                        hour = if (isAllDay) -1 else hour,
                        minute = minute,
                        color = existingEvent?.color ?: 0xFFFF6B6B.toInt(),
                        note = note,
                        reminderMinutes = existingEvent?.reminderMinutes ?: SettingsManager.defaultReminderMinutes
                    )
                    if (existingEvent != null) {
                        EventManager.updateEvent(event)
                    } else {
                        EventManager.addEvent(event)
                    }
                    onSaved()
                },
                enabled = title.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun YearPickerDialog(
    currentYear: Int,
    onYearSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val years = (1900..2100).toList()

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text("选择年份", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            val scrollState = rememberScrollState()
            LaunchedEffect(Unit) {
                val index = years.indexOf(currentYear)
                if (index >= 0) scrollState.scrollTo((index * 48) - 200)
            }

            Column(
                modifier = Modifier.height(300.dp).verticalScroll(scrollState)
            ) {
                years.forEach { year ->
                    val isSelected = year == currentYear
                    Surface(
                        onClick = { onYearSelected(year) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${year}年",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            if (isSelected) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

private fun getWeekdayName(year: Int, month: Int, day: Int): String {
    val cal = Calendar.getInstance()
    cal.set(year, month - 1, day)
    val weekDays = arrayOf("星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六")
    return weekDays[cal.get(Calendar.DAY_OF_WEEK) - 1]
}
