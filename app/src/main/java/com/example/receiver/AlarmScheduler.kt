package com.example.receiver

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.Alarm
import java.util.Calendar

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    @SuppressLint("ScheduleExactAlarm")
    fun schedule(alarm: Alarm) {
        if (!alarm.isEnabled) {
            cancel(alarm)
            return
        }

        val triggerTimeMs = getNextAlarmTimeInMillis(alarm)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGER
            putExtra(EXTRA_ALARM_ID, alarm.id)
            putExtra(EXTRA_ALARM_LABEL, alarm.label)
        }

        // We use FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE for security on API 31+
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTimeMs,
                            pendingIntent
                        )
                        Log.d("AlarmScheduler", "Scheduled exact alarm ${alarm.id} at $triggerTimeMs")
                    } else {
                        // Fallback if permission is missing
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTimeMs,
                            pendingIntent
                        )
                        Log.d("AlarmScheduler", "Scheduled inexact-idle alarm ${alarm.id} at $triggerTimeMs")
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMs,
                        pendingIntent
                    )
                    Log.d("AlarmScheduler", "Scheduled exact-idle alarm ${alarm.id} at $triggerTimeMs")
                }
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMs,
                    pendingIntent
                )
                Log.d("AlarmScheduler", "Scheduled exact alarm ${alarm.id} at $triggerTimeMs")
            }
        } catch (e: SecurityException) {
            Log.e("AlarmScheduler", "SecurityException scheduling exact alarm, falling back", e)
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMs,
                pendingIntent
            )
        }
    }

    fun cancel(alarm: Alarm) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d("AlarmScheduler", "Cancelled alarm ${alarm.id}")
        }
    }

    companion object {
        const val ACTION_ALARM_TRIGGER = "com.example.alarmclock.ALARM_TRIGGER"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_ALARM_LABEL = "extra_alarm_label"

        fun getNextAlarmTimeInMillis(alarm: Alarm): Long {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, alarm.hour)
                set(Calendar.MINUTE, alarm.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val daysList = alarm.getDaysList()
            val currentTime = System.currentTimeMillis()

            if (daysList.isEmpty()) {
                // One-time alarm
                if (calendar.timeInMillis <= currentTime) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
                return calendar.timeInMillis
            } else {
                // Map Mon=1 .. Sun=7 to Calendar days (Sun=1, Mon=2, ..., Sat=7)
                val entityToCalendarDay = mapOf(
                    1 to Calendar.MONDAY,
                    2 to Calendar.TUESDAY,
                    3 to Calendar.WEDNESDAY,
                    4 to Calendar.THURSDAY,
                    5 to Calendar.FRIDAY,
                    6 to Calendar.SATURDAY,
                    7 to Calendar.SUNDAY
                )

                val calendarDays = daysList.mapNotNull { entityToCalendarDay[it] }

                var bestCalendar = calendar
                var found = false
                for (i in 0..7) {
                    val checkCalendar = (calendar.clone() as Calendar).apply {
                        add(Calendar.DAY_OF_YEAR, i)
                    }
                    val checkDayOfWeek = checkCalendar.get(Calendar.DAY_OF_WEEK)
                    if (calendarDays.contains(checkDayOfWeek)) {
                        if (checkCalendar.timeInMillis > currentTime) {
                            bestCalendar = checkCalendar
                            found = true
                            break
                        }
                    }
                }
                if (!found) {
                    if (calendar.timeInMillis <= currentTime) {
                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                    }
                    return calendar.timeInMillis
                }
                return bestCalendar.timeInMillis
            }
        }
    }
}
