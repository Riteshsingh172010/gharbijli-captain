package me.gharbijli.partner

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class GharBijliPartnerApp : Application() {

    companion object {
        const val CHANNEL_BOOKING_DISPATCH = "gharbijli_booking_dispatch"
        lateinit var instance: GharBijliPartnerApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "New Booking Broadcasts"
            val descriptionText = "Urgent high-priority customer booking requests"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_BOOKING_DISPATCH, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }
}
