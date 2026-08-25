package com.calendar.cc

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class CalendarEvent(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int = -1,     // -1 表示全天
    val minute: Int = 0,
    val color: Int = 0xFFFF6B6B.toInt(),
    val reminderMinutes: Int = 10,  // 提前多少分钟提醒
    val note: String = ""
)

object EventManager {
    private const val PREFS_NAME = "calendar_events"
    private const val KEY_EVENTS = "events"

    private var prefs: SharedPreferences? = null
    private var eventsCache: MutableList<CalendarEvent> = mutableListOf()
    private var loaded = false

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadFromPrefs()
    }

    private fun loadFromPrefs() {
        eventsCache.clear()
        val json = prefs?.getString(KEY_EVENTS, "[]") ?: "[]"
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                eventsCache.add(CalendarEvent(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    year = obj.getInt("year"),
                    month = obj.getInt("month"),
                    day = obj.getInt("day"),
                    hour = obj.optInt("hour", -1),
                    minute = obj.optInt("minute", 0),
                    color = obj.optInt("color", 0xFFFF6B6B.toInt()),
                    reminderMinutes = obj.optInt("reminder", 10),
                    note = obj.optString("note", "")
                ))
            }
        } catch (_: Exception) {}
        loaded = true
    }

    private fun saveToPrefs() {
        val arr = JSONArray()
        eventsCache.forEach { ev ->
            val obj = JSONObject()
            obj.put("id", ev.id)
            obj.put("title", ev.title)
            obj.put("year", ev.year)
            obj.put("month", ev.month)
            obj.put("day", ev.day)
            obj.put("hour", ev.hour)
            obj.put("minute", ev.minute)
            obj.put("color", ev.color)
            obj.put("reminder", ev.reminderMinutes)
            obj.put("note", ev.note)
            arr.put(obj)
        }
        prefs?.edit()?.putString(KEY_EVENTS, arr.toString())?.apply()
    }

    fun addEvent(event: CalendarEvent) {
        eventsCache.add(event)
        saveToPrefs()
    }

    fun updateEvent(event: CalendarEvent) {
        val idx = eventsCache.indexOfFirst { it.id == event.id }
        if (idx >= 0) {
            eventsCache[idx] = event
            saveToPrefs()
        }
    }

    fun deleteEvent(id: String) {
        eventsCache.removeAll { it.id == id }
        saveToPrefs()
    }

    fun getEventsForDate(year: Int, month: Int, day: Int): List<CalendarEvent> {
        if (!loaded) loadFromPrefs()
        return eventsCache.filter { it.year == year && it.month == month && it.day == day }
            .sortedBy { if (it.hour < 0) 0 else it.hour * 60 + it.minute }
    }

    fun getEventsForMonth(year: Int, month: Int): List<CalendarEvent> {
        if (!loaded) loadFromPrefs()
        return eventsCache.filter { it.year == year && it.month == month }
    }

    fun hasEventsOnDay(year: Int, month: Int, day: Int): Boolean {
        if (!loaded) loadFromPrefs()
        return eventsCache.any { it.year == year && it.month == month && it.day == day }
    }

    fun getAllEvents(): List<CalendarEvent> {
        if (!loaded) loadFromPrefs()
        return eventsCache.toList().sortedBy { "${it.year}-${it.month.toString().padStart(2, '0')}-${it.day.toString().padStart(2, '0')}" }
    }
}

object SettingsManager {
    private const val PREFS_NAME = "calendar_settings"
    private const val KEY_WEEK_START = "week_start"      // 0=周日, 1=周一
    private const val KEY_DEFAULT_REMINDER = "default_reminder" // 分钟
    private const val KEY_DEFAULT_REMINDER_ENABLED = "reminder_enabled"
    private const val KEY_FIXED_TIMEZONE = "fixed_timezone"

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var weekStartDay: Int
        get() = prefs?.getInt(KEY_WEEK_START, 0) ?: 0
        set(value) { prefs?.edit()?.putInt(KEY_WEEK_START, value)?.apply() }

    var defaultReminderMinutes: Int
        get() = prefs?.getInt(KEY_DEFAULT_REMINDER, 10) ?: 10
        set(value) { prefs?.edit()?.putInt(KEY_DEFAULT_REMINDER, value)?.apply() }

    var reminderEnabled: Boolean
        get() = prefs?.getBoolean(KEY_DEFAULT_REMINDER_ENABLED, true) ?: true
        set(value) { prefs?.edit()?.putBoolean(KEY_DEFAULT_REMINDER_ENABLED, value)?.apply() }

    var fixedTimezone: Boolean
        get() = prefs?.getBoolean(KEY_FIXED_TIMEZONE, false) ?: false
        set(value) { prefs?.edit()?.putBoolean(KEY_FIXED_TIMEZONE, value)?.apply() }
}