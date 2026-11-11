package me.gharbijli.partner

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import me.gharbijli.partner.auth.LoginScreen
import me.gharbijli.partner.auth.RegisterScreen
import me.gharbijli.partner.bookings.BookingDetailsScreen
import me.gharbijli.partner.dashboard.DashboardScreen
import me.gharbijli.partner.data.PartnerApiService
import me.gharbijli.partner.data.SessionManager
import me.gharbijli.partner.earnings.EarningsScreen
import me.gharbijli.partner.ui.theme.GharBijliPartnerTheme
import me.gharbijli.partner.ui.theme.PrimaryBlue

class MainActivity : ComponentActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var apiService: PartnerApiService

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(applicationContext)
        apiService = PartnerApiService.create()

        // 1. Immediately request Notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val incomingBookingId = intent?.getStringExtra("bookingId")

        setContent {
            GharBijliPartnerTheme {
                val context = LocalContext.current
                var showPermissionDialog by remember { mutableStateOf(needsCriticalPermissions(context)) }

                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    val startDestination = if (sessionManager.isLoggedIn()) {
                        if (!incomingBookingId.isNullOrBlank()) "booking/$incomingBookingId" else "dashboard"
                    } else {
                        "login"
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        NavHost(navController = navController, startDestination = startDestination) {
                            composable("login") {
                                LoginScreen(
                                    sessionManager = sessionManager,
                                    apiService = apiService,
                                    onLoginSuccess = {
                                        navController.navigate("dashboard") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    },
                                    onNavigateToRegister = {
                                        navController.navigate("register")
                                    }
                                )
                            }

                            composable("register") {
                                RegisterScreen(
                                    sessionManager = sessionManager,
                                    apiService = apiService,
                                    onRegisterSuccess = {
                                        navController.popBackStack()
                                    },
                                    onNavigateBack = {
                                        navController.popBackStack()
                                    }
                                )
                            }

                            composable("dashboard") {
                                DashboardScreen(
                                    sessionManager = sessionManager,
                                    apiService = apiService,
                                    onNavigateToBooking = { bookingId ->
                                        navController.navigate("booking/$bookingId")
                                    },
                                    onNavigateToEarnings = {
                                        navController.navigate("earnings")
                                    },
                                    onLogout = {
                                        navController.navigate("login") {
                                            popUpTo("dashboard") { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable(
                                route = "booking/{bookingId}",
                                arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
                                BookingDetailsScreen(
                                    bookingId = bookingId,
                                    sessionManager = sessionManager,
                                    apiService = apiService,
                                    onNavigateBack = {
                                        navController.popBackStack()
                                    }
                                )
                            }

                            composable("earnings") {
                                EarningsScreen(
                                    sessionManager = sessionManager,
                                    apiService = apiService,
                                    onNavigateBack = {
                                        navController.popBackStack()
                                    }
                                )
                            }
                        }

                        // WhatsApp Style Incoming Call Permissions Dialog
                        if (showPermissionDialog) {
                            AlertDialog(
                                onDismissRequest = { showPermissionDialog = false },
                                title = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.PhoneInTalk,
                                            contentDescription = null,
                                            tint = PrimaryBlue,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Enable WhatsApp-Style Call Alerts",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp
                                        )
                                    }
                                },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(
                                            text = "To receive loud incoming booking calls and alarm rings even when your phone is locked or app is closed, please grant these permissions:",
                                            fontSize = 13.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        // 1. Overlay Permission Button
                                        if (!Settings.canDrawOverlays(context)) {
                                            Button(
                                                onClick = {
                                                    try {
                                                        val intent = Intent(
                                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                            Uri.parse("package:${context.packageName}")
                                                        )
                                                        context.startActivity(intent)
                                                    } catch (_: Exception) {}
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                                            ) {
                                                Icon(Icons.Default.PhoneInTalk, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("1. Allow Display Over Other Apps", fontSize = 12.5.sp)
                                            }
                                        }

                                        // 2. Battery Saver Exemption
                                        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                                        val isIgnoringBattery = powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
                                        if (!isIgnoringBattery) {
                                            Button(
                                                onClick = {
                                                    try {
                                                        val intent = Intent(
                                                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                                            Uri.parse("package:${context.packageName}")
                                                        )
                                                        context.startActivity(intent)
                                                    } catch (_: Exception) {}
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                                            ) {
                                                Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("2. Disable Battery Optimization", fontSize = 12.5.sp)
                                            }
                                        }

                                        // 3. Exact Alarm Permission (Android 12+)
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                                            if (alarmManager?.canScheduleExactAlarms() == false) {
                                                Button(
                                                    onClick = {
                                                        try {
                                                            val intent = Intent(
                                                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                                                Uri.parse("package:${context.packageName}")
                                                            )
                                                            context.startActivity(intent)
                                                        } catch (_: Exception) {}
                                                    },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                                                ) {
                                                    Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("3. Allow Alarms & Reminders", fontSize = 12.5.sp)
                                                }
                                            }
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { showPermissionDialog = false }) {
                                        Text("Continue to App", fontWeight = FontWeight.Bold)
                                    }
                                },
                                shape = RoundedCornerShape(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    private fun needsCriticalPermissions(context: Context): Boolean {
        val needsOverlay = !Settings.canDrawOverlays(context)
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val needsBattery = powerManager?.isIgnoringBatteryOptimizations(context.packageName) != true
        return needsOverlay || needsBattery
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
