package me.gharbijli.partner.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import me.gharbijli.partner.MainActivity
import me.gharbijli.partner.R
import me.gharbijli.partner.data.PartnerApiService
import me.gharbijli.partner.data.SessionManager
import me.gharbijli.partner.dispatch.IncomingCallActivity

class CaptainDutyService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isPolling = false
    private var lastSeenDispatchId: String? = null

    companion object {
        const val CHANNEL_DUTY = "gharbijli_duty_foreground"
        const val NOTIFICATION_ID = 1001

        fun startDuty(context: Context) {
            val intent = Intent(context, CaptainDutyService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopDuty(context: Context) {
            val intent = Intent(context, CaptainDutyService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createDutyNotificationChannel()
        startForeground(NOTIFICATION_ID, buildDutyNotification())
        startPollingForBookings()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startPollingForBookings()
        return START_STICKY
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

                            if (topRequest != null && topRequest.dispatchId != lastSeenDispatchId) {
                                lastSeenDispatchId = topRequest.dispatchId
                                launchIncomingCallScreen(topRequest)
                            } else if (topRequest == null) {
                                lastSeenDispatchId = null
                            }
                        }
                    } catch (e: Exception) {
                        // Silent retry on network hiccups
                    }
                } else if (!isOnline) {
                    stopSelf()
                    break
                }
                delay(3000)
            }
            isPolling = false
        }
    }

    private fun launchIncomingCallScreen(request: me.gharbijli.partner.data.DispatchRequest) {
        val intent = Intent(this, IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("dispatchId", request.dispatchId)
            putExtra("bookingId", request.bookingId)
            putExtra("serviceName", request.serviceName)
            putExtra("locality", request.locality)
            putExtra("address", request.customerAddress)
            putExtra("payoutAmount", request.payoutAmount)
            putExtra("distanceKm", request.distanceKm)
            putExtra("remainingSeconds", request.remainingSeconds)
        }
        startActivity(intent)
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

    private fun createDutyNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Captain Active Duty"
            val desc = "Keeps connection open for incoming customer bookings"
            val channel = NotificationChannel(CHANNEL_DUTY, name, NotificationManager.IMPORTANCE_LOW).apply {
                description = desc
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        isPolling = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
