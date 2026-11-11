package me.gharbijli.partner.service

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import me.gharbijli.partner.MainActivity
import me.gharbijli.partner.R
import me.gharbijli.partner.data.DispatchRequest
import me.gharbijli.partner.data.PartnerApiService
import me.gharbijli.partner.data.SessionManager
import me.gharbijli.partner.dispatch.IncomingCallActivity

class CaptainDutyService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isPolling = false
    private var lastSeenDispatchId: String? = null
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        const val CHANNEL_DUTY = "gharbijli_duty_foreground"
        const val CHANNEL_CALL = "gharbijli_call_high_priority"
        const val NOTIFICATION_ID_DUTY = 1001
        const val NOTIFICATION_ID_CALL = 2002

        fun startDuty(context: Context) {
            val intent = Intent(context, CaptainDutyService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {}
        }

        fun stopDuty(context: Context) {
            val intent = Intent(context, CaptainDutyService::class.java)
            try {
                context.stopService(intent)
            } catch (_: Exception) {}
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        startForeground(NOTIFICATION_ID_DUTY, buildDutyNotification())
        acquireWakeLock()
        startPollingForBookings()
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "GharBijliPartner:DutyServiceWakeLock"
            )?.apply {
                acquire(24 * 60 * 60 * 1000L /* 24 hours */)
            }
        } catch (_: Exception) {}
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startPollingForBookings()
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Electrician closed/swiped app - keep duty alive like WhatsApp
        scheduleServiceRestart()
    }

    private fun scheduleServiceRestart() {
        val sessionManager = SessionManager(applicationContext)
        if (sessionManager.isLoggedIn() && sessionManager.isOnline()) {
            try {
                val restartIntent = Intent(applicationContext, CaptainDutyService::class.java)
                val pendingIntent = PendingIntent.getService(
                    applicationContext,
                    901,
                    restartIntent,
                    PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                )
                val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                val triggerAt = System.currentTimeMillis() + 1500L
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager?.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                } else {
                    alarmManager?.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                }
            } catch (_: Exception) {}
        }
    }

    private fun startPollingForBookings() {
        if (isPolling) return
        isPolling = true

        val sessionManager = SessionManager(applicationContext)
        val apiService = PartnerApiService.create()

        serviceScope.launch {
            while (isActive) {
                val token = sessionManager.getToken()
                val isOnline = sessionManager.isOnline()

                if (!token.isNullOrBlank() && isOnline) {
                    try {
                        val authHeader = "Bearer $token"
                        val res = apiService.getPendingDispatches(authHeader)
                        if (res.isSuccessful && res.body()?.success == true) {
                            val requests = res.body()?.requests ?: emptyList()
                            val topRequest = requests.firstOrNull()

                            if (topRequest != null) {
                                val currentId = topRequest.dispatchId.ifBlank { topRequest.bookingId }
                                if (currentId != lastSeenDispatchId) {
                                    lastSeenDispatchId = currentId
                                    triggerWhatsAppStyleIncomingCall(topRequest)
                                }
                            } else {
                                if (lastSeenDispatchId != null) {
                                    dismissIncomingCallNotification()
                                    lastSeenDispatchId = null
                                }
                            }
                        }
                    } catch (_: Exception) {
                        // Silent retry on network fluctuations
                    }
                } else if (!isOnline) {
                    dismissIncomingCallNotification()
                    stopSelf()
                    break
                }
                delay(2500)
            }
            isPolling = false
        }
    }

    private fun triggerWhatsAppStyleIncomingCall(request: DispatchRequest) {
        val targetBookingId = request.bookingId.ifBlank { request.dispatchId.replace("disp_", "") }
        val targetDispatchId = request.dispatchId.ifBlank { "disp_$targetBookingId" }
        val payout = if (request.payoutAmount > 0) request.payoutAmount else 50

        // 1. Wake screen up immediately (turn on display if dark/locked)
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            @Suppress("DEPRECATION")
            val screenWakeLock = powerManager?.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                "GharBijliPartner:ScreenWakeLock"
            )
            screenWakeLock?.acquire(30000L) // 30 seconds
        } catch (_: Exception) {}

        // 2. Full Screen Intent for IncomingCallActivity
        val fullScreenIntent = Intent(this, IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            putExtra("dispatchId", targetDispatchId)
            putExtra("bookingId", targetBookingId)
            putExtra("serviceName", request.serviceName ?: "Electrical Service")
            putExtra("locality", request.locality ?: "Bhilai")
            putExtra("address", request.customerAddress ?: "Bhilai, Chhattisgarh")
            putExtra("payoutAmount", payout)
            putExtra("distanceKm", request.distanceKm)
            putExtra("remainingSeconds", request.remainingSeconds)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            targetDispatchId.hashCode(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Build High Priority Call Notification (Wakes device & rings like WhatsApp)
        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val callNotification = NotificationCompat.Builder(this, CHANNEL_CALL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("⚡ Incoming Booking: ${request.serviceName ?: "Electrical Service"}")
            .setContentText("₹$payout Captain Payout • ${request.locality ?: "Bhilai"}")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(false)
            .setOngoing(true)
            .setSound(ringtoneUri)
            .setVibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000))
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID_CALL, callNotification)

        // 4. Direct Activity Launch if Overlay Permission is granted or screen can be popped
        try {
            startActivity(fullScreenIntent)
        } catch (_: Exception) {}
    }

    private fun dismissIncomingCallNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(NOTIFICATION_ID_CALL)
    }

    private fun buildDutyNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_DUTY)
            .setContentTitle("⚡ GharBijli Captain Duty Active")
            .setContentText("Connected to Bhilai booking engine • Ready for customer calls")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            val dutyChannel = NotificationChannel(
                CHANNEL_DUTY,
                "Captain Active Duty",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps connection open for incoming customer bookings"
                setShowBadge(false)
            }

            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .build()

            val callChannel = NotificationChannel(
                CHANNEL_CALL,
                "Incoming Booking Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Full-screen loud alerts for incoming customer bookings"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)
                setSound(ringtoneUri, audioAttributes)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            manager?.createNotificationChannel(dutyChannel)
            manager?.createNotificationChannel(callChannel)
        }
    }

    override fun onDestroy() {
        scheduleServiceRestart()
        serviceScope.cancel()
        dismissIncomingCallNotification()
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (_: Exception) {}
        isPolling = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
