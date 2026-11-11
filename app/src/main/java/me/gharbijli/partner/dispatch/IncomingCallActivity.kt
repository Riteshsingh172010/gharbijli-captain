package me.gharbijli.partner.dispatch

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.gharbijli.partner.MainActivity
import me.gharbijli.partner.R
import me.gharbijli.partner.data.PartnerApiService
import me.gharbijli.partner.data.SessionManager
import me.gharbijli.partner.ui.theme.*

class IncomingCallActivity : ComponentActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Turn screen on and show over lock screen (like WhatsApp incoming call)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        startRingtoneAndVibration()

        val dispatchId = intent.getStringExtra("dispatchId") ?: ""
        val bookingId = intent.getStringExtra("bookingId") ?: ""
        val serviceName = intent.getStringExtra("serviceName") ?: "Electrical Service"
        val locality = intent.getStringExtra("locality") ?: "Bhilai"
        val address = intent.getStringExtra("address") ?: ""
        val payoutAmount = intent.getIntExtra("payoutAmount", 350)
        val distanceKm = intent.getDoubleExtra("distanceKm", 2.1)
        val remainingSeconds = intent.getIntExtra("remainingSeconds", 90)

        setContent {
            GharBijliPartnerTheme {
                IncomingCallScreenContent(
                    serviceName = serviceName,
                    locality = locality,
                    address = address,
                    payoutAmount = payoutAmount,
                    distanceKm = distanceKm,
                    remainingSeconds = remainingSeconds,
                    onAccept = {
                        stopRingtoneAndVibration()
                        val nm = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
                        nm?.cancel(me.gharbijli.partner.service.CaptainDutyService.NOTIFICATION_ID_CALL)
                        val sessionManager = SessionManager(applicationContext)
                        val token = "Bearer ${sessionManager.getToken() ?: ""}"
                        val apiService = PartnerApiService.create()
                        val targetId = if (!bookingId.isNullOrBlank()) bookingId else (dispatchId ?: "")

                        // Call accept
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            try {
                                apiService.acceptBooking(token, targetId)
                            } catch (_: Exception) {}
                        }

                        // Open main activity with booking
                        val mainIntent = Intent(this@IncomingCallActivity, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra("bookingId", targetId.replace("disp_", ""))
                        }
                        startActivity(mainIntent)
                        finish()
                    },
                    onDecline = {
                        stopRingtoneAndVibration()
                        val nm = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
                        nm?.cancel(me.gharbijli.partner.service.CaptainDutyService.NOTIFICATION_ID_CALL)
                        val sessionManager = SessionManager(applicationContext)
                        val token = "Bearer ${sessionManager.getToken() ?: ""}"
                        val apiService = PartnerApiService.create()
                        val targetId = if (!bookingId.isNullOrBlank()) bookingId else (dispatchId ?: "")

                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            try {
                                apiService.rejectBooking(token, targetId)
                            } catch (_: Exception) {}
                        }
                        finish()
                    }
                )
            }
        }
    }

    private fun startRingtoneAndVibration() {
        try {
            val ringtoneUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, ringtoneUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }

            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            val pattern = longArrayOf(0, 1000, 800, 1000, 800)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (_: Exception) {}
    }

    private fun stopRingtoneAndVibration() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            vibrator?.cancel()
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        stopRingtoneAndVibration()
        super.onDestroy()
    }
}

@Composable
fun IncomingCallScreenContent(
    serviceName: String,
    locality: String,
    address: String,
    payoutAmount: Int,
    distanceKm: Double,
    remainingSeconds: Int,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    var secondsLeft by remember { mutableStateOf(remainingSeconds.coerceAtLeast(10)) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft -= 1
        }
        onDecline()
    }

    // Pulsing animation for Accept button
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate900)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // New Captain Official Logo
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "GharBijli Captain",
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(8.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "⚡ INCOMING BOOKING CALL",
                color = AmberWarning,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Bhilai Customer Request",
                color = Slate300,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Service Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Slate800)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = serviceName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = EmeraldOnline, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$locality (~$distanceKm km)",
                            color = Slate100,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (address.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = address,
                            color = Slate400,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Slate700)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Your Net Earning", color = Slate400, fontSize = 12.sp)
                            Text("Direct Cash/UPI", color = Slate400, fontSize = 10.sp)
                        }
                        Text(
                            text = "₹$payoutAmount",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldOnline
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Seconds Countdown Badge
            Text(
                text = "Auto-declining in ${secondsLeft}s",
                color = if (secondsLeft > 15) Slate400 else RedOffline,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Bottom Call Action Buttons (Accept / Decline)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Red Decline Button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onDecline,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(RedDark)
                ) {
                    Icon(
                        Icons.Default.CallEnd,
                        contentDescription = "Decline",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Decline", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }

            // Green Accept Button (Pulsing)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onAccept,
                    modifier = Modifier
                        .size(76.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(EmeraldOnline)
                ) {
                    Icon(
                        Icons.Default.Call,
                        contentDescription = "Accept",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("ACCEPT JOB", color = EmeraldOnline, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
