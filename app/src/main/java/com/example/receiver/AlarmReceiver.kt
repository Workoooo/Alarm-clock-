package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.AlarmDatabase
import com.example.data.AlarmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        Log.d("AlarmReceiver", "onReceive action: $action")

        if (action == AlarmScheduler.ACTION_ALARM_TRIGGER) {
            val alarmId = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
            val alarmLabel = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_LABEL) ?: "Alarm"

            if (alarmId != -1) {
                val serviceIntent = Intent(context, AlarmService::class.java).apply {
                    this.action = AlarmService.ACTION_START
                    putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                    putExtra(AlarmScheduler.EXTRA_ALARM_LABEL, alarmLabel)
                }
                
                try {
                    ContextCompat.startForegroundService(context, serviceIntent)
                    Log.d("AlarmReceiver", "Successfully started AlarmService for alarm $alarmId")
                } catch (e: Exception) {
                    Log.e("AlarmReceiver", "Failed to start AlarmService", e)
                }
            }
        } else if (action == Intent.ACTION_BOOT_COMPLETED || action == "android.intent.action.QUICKBOOT_POWERON") {
            // Reschedule all active alarms upon reboot
            val database = AlarmDatabase.getDatabase(context.applicationContext)
            val repository = AlarmRepository(database.alarmDao())
            val scheduler = AlarmScheduler(context.applicationContext)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val alarms = repository.allAlarms.first()
                    for (alarm in alarms) {
                        if (alarm.isEnabled) {
                            scheduler.schedule(alarm)
                            Log.d("AlarmReceiver", "Rescheduled alarm ${alarm.id} on boot")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AlarmReceiver", "Error rescheduling alarms on boot", e)
                }
            }
        }
    }
}
