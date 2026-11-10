package me.gharbijli.partner

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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

        // Request notification permission for high-priority incoming call alerts
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val incomingBookingId = intent?.getStringExtra("bookingId")

        setContent {
            GharBijliPartnerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    val startDestination = if (sessionManager.isLoggedIn()) {
                        if (!incomingBookingId.isNullOrBlank()) "booking/$incomingBookingId" else "dashboard"
                    } else {
                        "login"
                    }

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
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
