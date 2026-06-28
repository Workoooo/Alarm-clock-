package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hour: Int, // 0 - 23
    val minute: Int, // 0 - 59
    val isEnabled: Boolean = true,
    val label: String = "",
    val isVibrate: Boolean = true,
    val isSound: Boolean = true,
    val daysOfWeek: String = "", // Comma-separated day numbers (1 = Mon, 7 = Sun). Empty means one-time alarm.
    val snoozeDurationMinutes: Int = 5
) {
    // Returns a list of integers representing active days of the week (1 to 7)
    fun getDaysList(): List<Int> {
        if (daysOfWeek.isEmpty()) return emptyList()
        return daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
    }

    // Helper to format days in a friendly readable string
    fun getDaysFormatted(): String {
        val daysList = getDaysList()
        if (daysList.isEmpty()) return "Once"
        if (daysList.size == 7) return "Every day"
        if (daysList.size == 5 && daysList.containsAll(listOf(1, 2, 3, 4, 5)) && !daysList.contains(6) && !daysList.contains(7)) {
            return "Weekdays"
        }
        if (daysList.size == 2 && daysList.containsAll(listOf(6, 7)) && !daysList.contains(1) && !daysList.contains(2) && !daysList.contains(3) && !daysList.contains(4) && !daysList.contains(5)) {
            return "Weekends"
        }
        
        val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        return daysList.sorted().map { dayNames[it - 1] }.joinToString(", ")
    }

    // Returns time formatted nicely (e.g., "08:30 AM")
    fun getFormattedTime(): String {
        val amPm = if (hour >= 12) "PM" else "AM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format("%02d:%02d %s", displayHour, minute, amPm)
    }
}
