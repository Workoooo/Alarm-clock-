package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Alarm
import com.example.data.AlarmDatabase
import com.example.data.AlarmRepository
import com.example.receiver.AlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class AlarmViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AlarmRepository
    private val scheduler = AlarmScheduler(application)

    // Flow of all alarms from the database
    val alarms: StateFlow<List<Alarm>>

    // Trigger state to periodically refresh next alarm time calculations (every minute)
    private val _refreshTrigger = MutableStateFlow(0)

    init {
        val database = AlarmDatabase.getDatabase(application)
        repository = AlarmRepository(database.alarmDao())
        
        alarms = repository.allAlarms.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Periodically refresh calculations so "Next alarm in Xm" updates every minute
        viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                delay(60000)
                _refreshTrigger.value = _refreshTrigger.value + 1
            }
        }
    }

    // Reactive computation of the remaining time until the soonest active alarm
    val nextAlarmTimeText: StateFlow<String> = _refreshTrigger.map { _ ->
        val activeAlarms = alarms.value.filter { it.isEnabled }
        if (activeAlarms.isEmpty()) {
            "All alarms turned off"
        } else {
            val now = System.currentTimeMillis()
            val nextTimes = activeAlarms.map { AlarmScheduler.getNextAlarmTimeInMillis(it) }
            val soonestTime = nextTimes.minOrNull() ?: 0L

            if (soonestTime > now) {
                val diffMs = soonestTime - now
                val diffSeconds = diffMs / 1000
                val diffMinutes = (diffSeconds / 60) % 60
                val diffHours = (diffSeconds / 3600) % 24
                val diffDays = diffSeconds / (3600 * 24)

                val parts = mutableListOf<String>()
                if (diffDays > 0) parts.add("${diffDays}d")
                if (diffHours > 0 || diffDays > 0) parts.add("${diffHours}h")
                parts.add("${diffMinutes}m")

                "Next alarm in " + parts.joinToString(" ")
            } else {
                "All alarms turned off"
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "No active alarms"
    )

    fun addAlarm(
        hour: Int,
        minute: Int,
        label: String,
        isVibrate: Boolean,
        isSound: Boolean,
        daysOfWeek: String,
        snoozeDuration: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val newAlarm = Alarm(
                hour = hour,
                minute = minute,
                isEnabled = true,
                label = label,
                isVibrate = isVibrate,
                isSound = isSound,
                daysOfWeek = daysOfWeek,
                snoozeDurationMinutes = snoozeDuration
            )
            val generatedId = repository.insertAlarm(newAlarm)
            val savedAlarm = newAlarm.copy(id = generatedId.toInt())
            scheduler.schedule(savedAlarm)
            // Trigger refresh
            _refreshTrigger.value = _refreshTrigger.value + 1
        }
    }

    fun toggleAlarm(alarm: Alarm) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedAlarm = alarm.copy(isEnabled = !alarm.isEnabled)
            repository.updateAlarm(updatedAlarm)
            
            if (updatedAlarm.isEnabled) {
                scheduler.schedule(updatedAlarm)
            } else {
                scheduler.cancel(updatedAlarm)
            }
            // Trigger refresh
            _refreshTrigger.value = _refreshTrigger.value + 1
        }
    }

    fun updateAlarm(alarm: Alarm) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateAlarm(alarm)
            if (alarm.isEnabled) {
                scheduler.schedule(alarm)
            } else {
                scheduler.cancel(alarm)
            }
            // Trigger refresh
            _refreshTrigger.value = _refreshTrigger.value + 1
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch(Dispatchers.IO) {
            scheduler.cancel(alarm)
            repository.deleteAlarm(alarm)
            // Trigger refresh
            _refreshTrigger.value = _refreshTrigger.value + 1
        }
    }
}
