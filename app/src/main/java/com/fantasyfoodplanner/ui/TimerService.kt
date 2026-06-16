package com.fantasyfoodplanner.ui

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import androidx.core.app.NotificationCompat
import com.fantasyfoodplanner.MainActivity
import java.util.concurrent.TimeUnit
import kotlin.math.max

class TimerService : Service() {

    private val binder = TimerBinder()
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val handler = Handler(Looper.getMainLooper())
    
    private var endTimeMs = 0L
    var remainingMs = 0L
    var isRunning = false
    var isAlarmActive = false
    var boundExerciseId: String? = null
    private var timerRunnable: Runnable? = null

    companion object {
        const val CHANNEL_ID = "TimerServiceChannel"
        const val NOTIFICATION_ID = 99
        const val ACTION_STOP_ALARM = "ACTION_STOP_ALARM"
    }

    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        vibrator = getSystemService(Vibrator::class.java)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Workout Timer",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_ALARM) {
            stopAlarm()
        }
        return START_STICKY
    }

    fun startTimer(exerciseId: String, durationSeconds: Int) {
        stopTimer()
        
        boundExerciseId = exerciseId
        val durationMs = durationSeconds * 1000L
        endTimeMs = SystemClock.elapsedRealtime() + durationMs
        remainingMs = durationMs
        isRunning = true
        
        val notification = buildNotification("Timer läuft: ${formatTime(remainingMs)}")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {}
        
        timerRunnable = object : Runnable {
            override fun run() {
                if (isRunning) {
                    val now = SystemClock.elapsedRealtime()
                    remainingMs = max(0L, endTimeMs - now)
                    updateNotification()
                    
                    if (remainingMs <= 0) {
                        onTimerFinished()
                    } else {
                        handler.postDelayed(this, 500) // Höhere Frequenz für flüssiges UI, aber ressourcenschonend
                    }
                }
            }
        }
        handler.post(timerRunnable!!)
    }

    fun stopTimer() {
        isRunning = false
        timerRunnable?.let { handler.removeCallbacks(it) }
        timerRunnable = null
        boundExerciseId = null
        stopAlarm()
        stopForeground(STOP_FOREGROUND_REMOVE)
        // Wir stoppen den Service NICHT komplett mit stopSelf(), 
        // damit das Binding für die nächste Übung bestehen bleibt,
        // außer es gibt wirklich keinen Timer mehr.
    }

    private fun onTimerFinished() {
        isRunning = false
        isAlarmActive = true
        startAlarm()
        updateNotification("ZEIT ABGELAUFEN!")
    }

    private fun startAlarm() {
        try {
            safeReleasePlayer()
            val alarmUri: Uri = android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI
            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, alarmUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            try {
                mediaPlayer = MediaPlayer.create(this, android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI)
                mediaPlayer?.isLooping = true
                mediaPlayer?.start()
            } catch (e2: Exception) {}
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 500), 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 500, 500), 0)
            }
        } catch (e: Exception) {}
    }

    private fun safeReleasePlayer() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {}
        mediaPlayer = null
    }

    fun stopAlarm() {
        isAlarmActive = false
        safeReleasePlayer()
        try {
            vibrator?.cancel()
        } catch (e: Exception) {}
        if (!isRunning) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }

    private fun updateNotification(text: String? = null) {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildNotification(text ?: formatTime(remainingMs)))
    }

    private fun buildNotification(content: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val stopIntent = Intent(this, TimerService::class.java).apply { action = ACTION_STOP_ALARM }
        val stopPendingIntent = PendingIntent.getService(this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Fantasy Planner Timer")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .setOngoing(isRunning || isAlarmActive)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun formatTime(ms: Long): String {
        val min = TimeUnit.MILLISECONDS.toMinutes(ms)
        val sec = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
        return "%02d:%02d".format(min, sec)
    }

    override fun onDestroy() {
        stopTimer()
        super.onDestroy()
    }
}
