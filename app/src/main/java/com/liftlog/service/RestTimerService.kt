package com.liftlog.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.liftlog.MainActivity
import com.liftlog.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class RestTimerState(
    val isRunning: Boolean = false,
    val targetEndTs: Long = 0L,
    val totalDurationSec: Int = 0,
    val exerciseName: String = "",
    val remainingSec: Int = 0,
    val isFinished: Boolean = false
)

class RestTimerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var countdownJob: Job? = null
    private lateinit var notificationManager: NotificationManager
    private var hasWarned10s = false

    companion object {
        const val CHANNEL_ID = "liftlog_rest_timer_channel"
        const val CHANNEL_ALERT_ID = "liftlog_rest_timer_alert_channel"
        const val NOTIFICATION_ID = 1001
        const val ALERT_NOTIFICATION_ID = 1002

        const val ACTION_START = "com.liftlog.action.START_TIMER"
        const val ACTION_ADD_15 = "com.liftlog.action.ADD_15"
        const val ACTION_SUB_15 = "com.liftlog.action.SUB_15"
        const val ACTION_SKIP = "com.liftlog.action.SKIP"
        const val ACTION_STOP = "com.liftlog.action.STOP"

        const val EXTRA_SECONDS = "extra_seconds"
        const val EXTRA_EXERCISE_NAME = "extra_exercise_name"

        private val _timerState = MutableStateFlow(RestTimerState())
        val timerState: StateFlow<RestTimerState> = _timerState.asStateFlow()

        fun startTimer(context: Context, seconds: Int, exerciseName: String) {
            val intent = Intent(context, RestTimerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SECONDS, seconds)
                putExtra(EXTRA_EXERCISE_NAME, exerciseName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun add15Seconds(context: Context) {
            val intent = Intent(context, RestTimerService::class.java).apply { action = ACTION_ADD_15 }
            context.startService(intent)
        }

        fun sub15Seconds(context: Context) {
            val intent = Intent(context, RestTimerService::class.java).apply { action = ACTION_SUB_15 }
            context.startService(intent)
        }

        fun skipTimer(context: Context) {
            val intent = Intent(context, RestTimerService::class.java).apply { action = ACTION_SKIP }
            context.startService(intent)
        }

        fun stopTimer(context: Context) {
            val intent = Intent(context, RestTimerService::class.java).apply { action = ACTION_STOP }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val seconds = intent.getIntExtra(EXTRA_SECONDS, 90)
                val exName = intent.getStringExtra(EXTRA_EXERCISE_NAME) ?: "Rest"
                val endTs = System.currentTimeMillis() + (seconds * 1000L)
                hasWarned10s = false
                _timerState.value = RestTimerState(
                    isRunning = true,
                    targetEndTs = endTs,
                    totalDurationSec = seconds,
                    exerciseName = exName,
                    remainingSec = seconds,
                    isFinished = false
                )
                startForeground(NOTIFICATION_ID, buildNotification(seconds, exName))
                startCountdownLoop()
            }
            ACTION_ADD_15 -> {
                val current = _timerState.value
                if (current.isRunning) {
                    val newEnd = current.targetEndTs + 15_000L
                    val newRemaining = maxOf(0, ((newEnd - System.currentTimeMillis()) / 1000L).toInt())
                    if (newRemaining > 10) {
                        hasWarned10s = false
                    }
                    _timerState.value = current.copy(
                        targetEndTs = newEnd,
                        remainingSec = newRemaining,
                        totalDurationSec = current.totalDurationSec + 15
                    )
                    updateNotification(newRemaining, current.exerciseName)
                }
            }
            ACTION_SUB_15 -> {
                val current = _timerState.value
                if (current.isRunning) {
                    val newEnd = current.targetEndTs - 15_000L
                    val newRemaining = maxOf(0, ((newEnd - System.currentTimeMillis()) / 1000L).toInt())
                    if (newRemaining <= 0) {
                        onTimerFinished(current.exerciseName)
                    } else {
                        _timerState.value = current.copy(
                            targetEndTs = newEnd,
                            remainingSec = newRemaining
                        )
                        updateNotification(newRemaining, current.exerciseName)
                    }
                }
            }
            ACTION_SKIP, ACTION_STOP -> {
                stopTimer()
            }
        }
        return START_STICKY
    }

    private fun startCountdownLoop() {
        countdownJob?.cancel()
        countdownJob = serviceScope.launch {
            while (isActive) {
                val current = _timerState.value
                if (!current.isRunning) break

                val now = System.currentTimeMillis()
                val remaining = maxOf(0, ((current.targetEndTs - now) / 1000L).toInt())
                _timerState.value = current.copy(remainingSec = remaining)

                if (remaining <= 0) {
                    onTimerFinished(current.exerciseName)
                    break
                } else {
                    if (remaining in 1..10 && !hasWarned10s && current.totalDurationSec > 15) {
                        hasWarned10s = true
                        playWarningPip()
                    }
                    updateNotification(remaining, current.exerciseName)
                }
                delay(1000L)
            }
        }
    }

    private fun playWarningPip() {
        try {
            // Light 60ms vibration cue
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
            }

            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val focusRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                            .build()
                    )
                    .setAcceptsDelayedFocusGain(false)
                    .build()
            } else null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) {
                audioManager.requestAudioFocus(focusRequest)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(null, AudioManager.STREAM_NOTIFICATION, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            }

            val toneGen = android.media.ToneGenerator(AudioManager.STREAM_NOTIFICATION, 75)
            toneGen.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 180)

            serviceScope.launch {
                delay(500L)
                toneGen.release()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) {
                    audioManager.abandonAudioFocusRequest(focusRequest)
                } else {
                    @Suppress("DEPRECATION")
                    audioManager.abandonAudioFocus(null)
                }
            }
        } catch (_: Exception) {}
    }

    private fun onTimerFinished(exerciseName: String) {
        _timerState.value = _timerState.value.copy(
            isRunning = false,
            remainingSec = 0,
            isFinished = true
        )
        triggerGentleVibration()
        playChimeWithMusicDucking()

        val alertSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val finishedNotification = NotificationCompat.Builder(this, CHANNEL_ALERT_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Rest Complete")
            .setContentText("Ready for: $exerciseName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(alertSound)
            .setVibrate(longArrayOf(0, 150, 100, 150))
            .setAutoCancel(true)
            .setContentIntent(createOpenAppPendingIntent())
            .build()

        notificationManager.notify(ALERT_NOTIFICATION_ID, finishedNotification)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun playChimeWithMusicDucking() {
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val focusRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                            .build()
                    )
                    .setAcceptsDelayedFocusGain(false)
                    .build()
            } else null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) {
                audioManager.requestAudioFocus(focusRequest)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(null, AudioManager.STREAM_NOTIFICATION, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            }

            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringtone = RingtoneManager.getRingtone(applicationContext, soundUri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtone.isLooping = false
                ringtone.volume = 1.0f
            }
            ringtone.play()

            serviceScope.launch {
                delay(2000L)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) {
                    audioManager.abandonAudioFocusRequest(focusRequest)
                } else {
                    @Suppress("DEPRECATION")
                    audioManager.abandonAudioFocus(null)
                }
            }
        } catch (_: Exception) {}
    }

    private fun triggerGentleVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 150, 100, 150), -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 150, 100, 150), -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 150, 100, 150), -1)
                }
            }
        } catch (_: Exception) {}
    }

    private fun stopTimer() {
        countdownJob?.cancel()
        _timerState.value = RestTimerState()
        notificationManager.cancel(NOTIFICATION_ID)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateNotification(remainingSec: Int, exerciseName: String) {
        notificationManager.notify(NOTIFICATION_ID, buildNotification(remainingSec, exerciseName))
    }

    private fun buildNotification(remainingSec: Int, exerciseName: String): android.app.Notification {
        val minutes = remainingSec / 60
        val seconds = remainingSec % 60
        val timeFormatted = String.format("%02d:%02d", minutes, seconds)

        val sub15Intent = PendingIntent.getService(
            this, 1,
            Intent(this, RestTimerService::class.java).apply { action = ACTION_SUB_15 },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val add15Intent = PendingIntent.getService(
            this, 2,
            Intent(this, RestTimerService::class.java).apply { action = ACTION_ADD_15 },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val skipIntent = PendingIntent.getService(
            this, 3,
            Intent(this, RestTimerService::class.java).apply { action = ACTION_SKIP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Rest: $exerciseName ($timeFormatted)")
            .setContentText("Tap to return to workout")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(createOpenAppPendingIntent())
            .addAction(0, "-15s", sub15Intent)
            .addAction(0, "+15s", add15Intent)
            .addAction(0, "Skip", skipIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createOpenAppPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val countdownChannel = NotificationChannel(
                CHANNEL_ID,
                "Workout Rest Timer (Countdown)",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows remaining rest time between workout sets"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(countdownChannel)

            val alertSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()

            val alertChannel = NotificationChannel(
                CHANNEL_ALERT_ID,
                "Rest Complete Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when rest timer finishes with sound and vibration"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 150, 100, 150)
                setSound(alertSound, audioAttributes)
            }
            notificationManager.createNotificationChannel(alertChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        countdownJob?.cancel()
        super.onDestroy()
    }
}
