package com.calendar.cc

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calendar.cc.ui.theme.*

enum class AppScreen(val label: String, val icon: ImageVector) {
    CALENDAR("日历", Icons.Filled.CalendarMonth),
    SEARCH("搜索", Icons.Filled.Search),
    SETTINGS("设置", Icons.Filled.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarApp() {
    var currentScreen by remember { mutableStateOf(AppScreen.CALENDAR) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                AppScreen.entries.forEach { screen ->
                    NavigationBarItem(
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(screen.icon, contentDescription = screen.label)
                        },
                        label = {
                            Text(screen.label, style = MaterialTheme.typography.labelSmall)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentScreen) {
                AppScreen.CALENDAR -> CalendarScreen()
                AppScreen.SEARCH -> SearchScreen()
                AppScreen.SETTINGS -> SettingsScreen()
            }
        }
    }
}

@Composable
fun SearchScreen() {
    var refreshKey by remember { mutableIntStateOf(0) }
    val allEvents = remember(refreshKey) { EventManager.getAllEvents() }
    var searchQuery by remember { mutableStateOf("") }
    val filteredEvents = remember(searchQuery, allEvents) {
        if (searchQuery.isBlank()) allEvents
        else allEvents.filter { ev ->
            ev.title.contains(searchQuery, ignoreCase = true) ||
            ev.note.contains(searchQuery, ignoreCase = true)
        }
    }
    var editingEvent by remember { mutableStateOf<CalendarEvent?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("搜索日程…") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Filled.Clear, contentDescription = "清除")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        Spacer(Modifier.height(16.dp))

        if (filteredEvents.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.EventBusy,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (searchQuery.isBlank()) "暂无日程" else "未找到匹配日程",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            Text(
                "共 ${filteredEvents.size} 项日程",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            filteredEvents.forEach { event ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { editingEvent = event },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(40.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .drawWithContent {
                                    drawRoundRect(
                                        color = Color(event.color),
                                        cornerRadius = CornerRadius(2.dp.toPx())
                                    )
                                }
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                event.title,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Text(
                                "${event.year}年${event.month}月${event.day}日" +
                                        if (event.hour >= 0) " ${event.hour.toString().padStart(2, '0')}:${event.minute.toString().padStart(2, '0')}" else " 全天",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = {
                                EventManager.deleteEvent(event.id)
                                refreshKey++
                            }
                        ) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = "删除",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    // 编辑日程弹窗
    editingEvent?.let { event ->
        AddEventDialog(
            year = event.year,
            month = event.month,
            day = event.day,
            existingEvent = event,
            onDismiss = { editingEvent = null },
            onSaved = {
                editingEvent = null
                refreshKey++
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val weekStart = remember { mutableStateOf(SettingsManager.weekStartDay) }
    val reminderEnabled = remember { mutableStateOf(SettingsManager.reminderEnabled) }
    val defaultReminder = remember { mutableStateOf(SettingsManager.defaultReminderMinutes) }
    val fixedTz = remember { mutableStateOf(SettingsManager.fixedTimezone) }
    var showClearDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            "设置",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(24.dp))

        SettingsSection("视图") {
            SettingsRow(
                title = "一周开始日",
                subtitle = if (weekStart.value == 0) "周日" else "周一",
                trailing = {
                    Switch(
                        checked = weekStart.value == 1,
                        onCheckedChange = {
                            weekStart.value = if (it) 1 else 0
                            SettingsManager.weekStartDay = weekStart.value
                        }
                    )
                }
            )

            SettingsRow(
                title = "固定时区",
                subtitle = "将日程时间和日期固定在所选时区",
                trailing = {
                    Switch(
                        checked = fixedTz.value,
                        onCheckedChange = {
                            fixedTz.value = it
                            SettingsManager.fixedTimezone = it
                        }
                    )
                }
            )
        }

        Spacer(Modifier.height(16.dp))

        SettingsSection("提醒") {
            SettingsRow(
                title = "内容与资讯通知",
                trailing = {
                    Switch(
                        checked = reminderEnabled.value,
                        onCheckedChange = {
                            reminderEnabled.value = it
                            SettingsManager.reminderEnabled = it
                        }
                    )
                }
            )

            SettingsRow(
                title = "默认提醒方式",
                subtitle = "提前 ${defaultReminder.value} 分钟",
                trailing = {
                    var showPicker by remember { mutableStateOf(false) }
                    TextButton(onClick = { showPicker = true }) {
                        Text("修改")
                    }
                    if (showPicker) {
                        AlertDialog(
                            onDismissRequest = { showPicker = false },
                            title = { Text("默认提醒时间") },
                            text = {
                                val options = listOf(0, 5, 10, 15, 30, 60)
                                Column {
                                    options.forEach { min ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    defaultReminder.value = min
                                                    SettingsManager.defaultReminderMinutes = min
                                                    showPicker = false
                                                }
                                                .padding(vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = defaultReminder.value == min,
                                                onClick = {
                                                    defaultReminder.value = min
                                                    SettingsManager.defaultReminderMinutes = min
                                                    showPicker = false
                                                }
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                if (min == 0) "不提醒" else "提前 ${min} 分钟"
                                            )
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showPicker = false }) {
                                    Text("取消")
                                }
                            }
                        )
                    }
                }
            )
        }

        Spacer(Modifier.height(16.dp))

        SettingsSection("数据管理") {
            SettingsRow(
                title = "清除过期日程",
                subtitle = "删除已过期的日程",
                trailing = {
                    TextButton(onClick = { showClearDialog = true }) {
                        Text("清除", color = MaterialTheme.colorScheme.error)
                    }
                }
            )

            SettingsRow(
                title = "清除相同日程",
                subtitle = "删除重复的日程",
                trailing = {
                    TextButton(onClick = {
                        val all = EventManager.getAllEvents()
                        val seen = mutableSetOf<String>()
                        all.forEach { ev ->
                            val key = "${ev.title}|${ev.year}|${ev.month}|${ev.day}|${ev.hour}|${ev.minute}"
                            if (key in seen) EventManager.deleteEvent(ev.id)
                            else seen.add(key)
                        }
                    }) {
                        Text("清除", color = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }

        Spacer(Modifier.height(16.dp))

        SettingsSection("订阅") {
            Text(
                "订阅服务与日程同步功能即将上线",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            )
        }

        Spacer(Modifier.height(32.dp))
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清除过期日程") },
            text = { Text("确定要删除所有已过期的日程吗？") },
            confirmButton = {
                TextButton(onClick = {
                    val now = java.util.Calendar.getInstance()
                    val today = "${now.get(java.util.Calendar.YEAR)}-${(now.get(java.util.Calendar.MONTH)+1).toString().padStart(2,'0')}-${now.get(java.util.Calendar.DAY_OF_MONTH).toString().padStart(2,'0')}"
                    val all = EventManager.getAllEvents()
                    all.forEach { ev ->
                        val dateStr = "${ev.year}-${ev.month.toString().padStart(2,'0')}-${ev.day.toString().padStart(2,'0')}"
                        if (dateStr < today) EventManager.deleteEvent(ev.id)
                    }
                    showClearDialog = false
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
    )
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 4.dp), content = content)
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) trailing()
    }
}
