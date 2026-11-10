package me.gharbijli.partner.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.gharbijli.partner.R
import me.gharbijli.partner.data.*
import me.gharbijli.partner.dispatch.IncomingBroadcastDialog
import me.gharbijli.partner.service.CaptainDutyService
import me.gharbijli.partner.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    sessionManager: SessionManager,
    apiService: PartnerApiService,
    onNavigateToBooking: (String) -> Unit,
    onNavigateToEarnings: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var isOnline by remember { mutableStateOf(sessionManager.isOnline()) }
    var partnerProfile by remember { mutableStateOf<PartnerProfile?>(null) }
    var activeBookings by remember { mutableStateOf<List<BookingDetail>>(emptyList()) }
    var pendingRequest by remember { mutableStateOf<DispatchRequest?>(null) }
    var isToggling by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val token = "Bearer ${sessionManager.getToken() ?: ""}"

    // Initial load
    fun refreshData() {
        scope.launch {
            try {
                val pRes = apiService.getProfile(token)
                if (pRes.isSuccessful && pRes.body()?.success == true) {
                    val p = pRes.body()?.partner
                    partnerProfile = p
                    if (p != null) {
                        isOnline = p.isOnline
                        sessionManager.setOnline(p.isOnline)
                    }
                }
                val bRes = apiService.getActiveBookings(token)
                if (bRes.isSuccessful) {
                    activeBookings = bRes.body()?.bookings ?: emptyList()
                }
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

    // Duty Service & Polling for broadcast requests when online
    LaunchedEffect(isOnline) {
        if (isOnline) {
            CaptainDutyService.startDuty(context)
        } else {
            CaptainDutyService.stopDuty(context)
        }
        while (isOnline) {
            try {
                val res = apiService.getPendingDispatches(token)
                if (res.isSuccessful && res.body()?.success == true) {
                    val list = res.body()?.requests ?: emptyList()
                    pendingRequest = list.firstOrNull()
                }
            } catch (_: Exception) {}
            delay(3000)
        }
        pendingRequest = null
    }

    // Incoming Broadcast Dialog
    pendingRequest?.let { req ->
        IncomingBroadcastDialog(
            request = req,
            onAccept = {
                scope.launch {
                    try {
                        val res = apiService.acceptBooking(token, req.dispatchId)
                        if (res.isSuccessful && res.body()?.success == true) {
                            pendingRequest = null
                            val bId = res.body()?.booking?.bookingId ?: req.bookingId
                            refreshData()
                            onNavigateToBooking(bId)
                        } else {
                            pendingRequest = null
                            refreshData()
                        }
                    } catch (_: Exception) {
                        pendingRequest = null
                    }
                }
            },
            onReject = {
                scope.launch {
                    try {
                        apiService.rejectBooking(token, req.dispatchId)
                    } catch (_: Exception) {}
                    pendingRequest = null
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = "Captain Logo",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.dp, Slate200, CircleShape),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = partnerProfile?.name ?: sessionManager.getPartnerName(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Slate900
                            )
                            Text(
                                text = "GharBijli Captain • Bhilai",
                                fontSize = 11.sp,
                                color = Slate500
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        sessionManager.clearSession()
                        onLogout()
                    }) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout", tint = Slate600)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Slate50
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. Online / Offline Duty Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isOnline) EmeraldLight else Color.White
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (isOnline) EmeraldDark else Slate200
                        )
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (isOnline) EmeraldOnline else Slate500)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isOnline) "ONLINE • DUTY ACTIVE" else "OFFLINE • OFF DUTY",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOnline) EmeraldDark else Slate600
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isOnline) "Receiving nearby booking broadcasts" else "Toggle ON to receive customer bookings",
                                fontSize = 12.sp,
                                color = Slate600
                            )
                        }

                        Switch(
                            checked = isOnline,
                            onCheckedChange = { targetOnline ->
                                if (!isToggling) {
                                    isToggling = true
                                    scope.launch {
                                        try {
                                            val res = apiService.toggleAvailability(token, ToggleAvailabilityRequest(targetOnline))
                                            if (res.isSuccessful) {
                                                isOnline = targetOnline
                                                sessionManager.setOnline(targetOnline)
                                            }
                                        } catch (_: Exception) {}
                                        isToggling = false
                                    }
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = EmeraldOnline,
                                uncheckedThumbColor = Slate500,
                                uncheckedTrackColor = Slate200
                            )
                        )
                    }
                }
            }

            // 2. Performance & Quick Stats
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Today Earnings Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToEarnings() },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Today's Payout", fontSize = 12.sp, color = Slate500)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "₹${partnerProfile?.todayEarnings ?: 0}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Tap for details →", fontSize = 11.sp, color = PrimaryBlue)
                        }
                    }

                    // Jobs & Rating Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Jobs Completed", fontSize = 12.sp, color = Slate500)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${partnerProfile?.totalJobsCompleted ?: 0}",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "⭐ ${partnerProfile?.rating ?: 4.9}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AmberWarning
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("High Captain Rating", fontSize = 11.sp, color = Slate500)
                        }
                    }
                }
            }

            // 3. Active Bookings Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active Jobs (${activeBookings.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    IconButton(onClick = { refreshData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = PrimaryBlue)
                    }
                }
            }

            if (activeBookings.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.ElectricBolt,
                                contentDescription = null,
                                tint = Slate300,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (isOnline) "Waiting for incoming jobs..." else "You are currently offline",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Slate600
                            )
                            Text(
                                text = if (isOnline) "Stay on this screen. New bookings will alert immediately." else "Turn duty switch ON to receive customer bookings.",
                                fontSize = 12.sp,
                                color = Slate500,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            } else {
                items(activeBookings.size) { idx ->
                    val booking = activeBookings[idx]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToBooking(booking.bookingId) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = booking.serviceName ?: "Electrical Repair",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Text(
                                    text = "₹${booking.finalAmount}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldDark
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Slate500, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${booking.customerName ?: "Customer"} • ${booking.locality ?: "Bhilai"}",
                                    fontSize = 13.sp,
                                    color = Slate600
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Status: ${booking.bookingStatus}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PrimaryBlue
                                )
                                Button(
                                    onClick = { onNavigateToBooking(booking.bookingId) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text("Open Job", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}
