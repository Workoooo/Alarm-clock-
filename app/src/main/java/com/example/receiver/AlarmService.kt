package com.example.receiver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.Vibrator
import android.os.VibratorManager
import android.os.VibrationEffect
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.Alarm
import com.example.data.AlarmDatabase
import com.example.data.AlarmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Calendar

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val alarmId = intent?.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1) ?: -1
        val alarmLabel = intent?.getStringExtra(AlarmScheduler.EXTRA_ALARM_LABEL) ?: "Alarm"

        Log.d("AlarmService", "onStartCommand action: $action, alarmId: $alarmId")

        if (alarmId == -1) {
            stopSelf()
            return START_NOT_STICKY
        }

        when (action) {
            ACTION_START -> {
                startAlarm(alarmId, alarmLabel)
            }
            ACTION_DISMISS -> {
                dismissAlarm(alarmId)
            }
            ACTION_SNOOZE -> {
                snoozeAlarm(alarmId)
            }
            else -> {
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun startAlarm(alarmId: Int, alarmLabel: String) {
        // Build the foreground notification
        val notification = createNotification(alarmId, alarmLabel)
        startForeground(NOTIFICATION_ID, notification)

        // Start audio and vibration
        playRingtone()
        startVibration()
    }

    private fun dismissAlarm(alarmId: Int) {
        stopAudioAndVibration()
        
        // Update database: if it's a one-time alarm, set isEnabled = false
        val database = AlarmDatabase.getDatabase(applicationContext)
        val repository = AlarmRepository(database.alarmDao())

        serviceScope.launch {
            val alarm = repository.getAlarmById(alarmId)
            if (alarm != null) {
                if (alarm.daysOfWeek.isEmpty()) {
                    // One-time alarm: disable it
                    repository.updateAlarm(alarm.copy(isEnabled = false))
                } else {
                    // Repeating alarm: reschedule next occurrence
                    val scheduler = AlarmScheduler(applicationContext)
                    scheduler.schedule(alarm)
                }
            }
            stopForeground(true)
            stopSelf()
        }
    }

    private fun snoozeAlarm(alarmId: Int) {
        stopAudioAndVibration()

        val database = AlarmDatabase.getDatabase(applicationContext)
        val repository = AlarmRepository(database.alarmDao())

        serviceScope.launch {
            val alarm = repository.getAlarmById(alarmId)
            if (alarm != null) {
                // Schedule a snooze alarm (as a one-off temporary trigger) using AlarmManager
                val snoozeMinutes = alarm.snoozeDurationMinutes
                val calendar = Calendar.getInstance().apply {
                    add(Calendar.MINUTE, snoozeMinutes)
                }

                val snoozeAlarm = Alarm(
                    id = alarmId + 100000, // Safe offset for temporary snooze alarm
                    hour = calendar.get(Calendar.HOUR_OF_DAY),
                    minute = calendar.get(Calendar.MINUTE),
                    isEnabled = true,
                    label = "${alarm.label} (Snoozed)",
                    isVibrate = alarm.isVibrate,
                    isSound = alarm.isSound,
                    daysOfWeek = "", // One-time
                    snoozeDurationMinutes = alarm.snoozeDurationMinutes
                )

                val scheduler = AlarmScheduler(applicationContext)
                scheduler.schedule(snoozeAlarm)
                Log.d("AlarmService", "Scheduled snooze alarm for ${snoozeMinutes} minutes from now")
            }
            stopForeground(true)
            stopSelf()
        }
    }

    private fun playRingtone() {
        if (mediaPlayer != null) return

        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(applicationContext, ringtoneUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            } catch (e: IOException) {
                Log.e("AlarmService", "Failed to prepare MediaPlayer", e)
            }
        }
    }

    private fun startVibration() {
        if (vibrator != null) return

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        vibrator?.let { v ->
            if (v.hasVibrator()) {
                val pattern = longArrayOf(0, 500, 500) // Vibrate 500ms, pause 500ms
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(pattern, 0)
                }
            }
        }
    }

    private fun stopAudioAndVibration() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null

        vibrator?.cancel()
        vibrator = null
    }

    override fun onDestroy() {
        stopAudioAndVibration()
        super.onDestroy()
    }

    private fun createNotification(alarmId: Int, alarmLabel: String): Notification {
        // Clicking notification opens MainActivity
        val contentIntent = Intent(this, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            alarmId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss action
        val dismissIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_DISMISS
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
        }
        val dismissPendingIntent = PendingIntent.getService(
            this,
            alarmId + 100,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze action
        val snoozeIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
        }
        val snoozePendingIntent = PendingIntent.getService(
            this,
            alarmId + 200,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Alarm Ringing!")
            .setContentText(alarmLabel)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)
            .addAction(android.R.drawable.ic_menu_recent_history, "Snooze", snoozePendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarm Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for ringing alarm notifications"
                enableVibration(true)
                setSound(null, null) // Handled by service MediaPlayer
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "alarm_ringing_channel"
        const val NOTIFICATION_ID = 8888
        
        const val ACTION_START = "com.example.alarmclock.ACTION_START"
        const val ACTION_DISMISS = "com.example.alarmclock.ACTION_DISMISS"
        const val ACTION_SNOOZE = "com.example.alarmclock.ACTION_SNOOZE"
    }
}
