package me.gharbijli.partner.bookings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import me.gharbijli.partner.data.*
import me.gharbijli.partner.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailsScreen(
    bookingId: String,
    sessionManager: SessionManager,
    apiService: PartnerApiService,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val token = "Bearer ${sessionManager.getToken() ?: ""}"

    var booking by remember { mutableStateOf<BookingDetail?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var actionLoading by remember { mutableStateOf(false) }
    var partsCostInput by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    fun loadBooking() {
        scope.launch {
            try {
                val res = apiService.getBookingDetail(token, bookingId)
                if (res.isSuccessful) {
                    booking = res.body()?.booking
                }
            } catch (_: Exception) {}
            isLoading = false
        }
    }

    LaunchedEffect(bookingId) {
        loadBooking()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Job #${bookingId.takeLast(6)}", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Slate50
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else if (booking == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Booking not found", color = Slate600)
            }
        } else {
            val b = booking!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Status Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (b.bookingStatus) {
                            "COMPLETED" -> EmeraldLight
                            "ON_THE_WAY", "ARRIVED", "IN_PROGRESS" -> PrimaryBlueLight
                            else -> AmberLight
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (b.bookingStatus) {
                                "COMPLETED" -> Icons.Default.CheckCircle
                                "ON_THE_WAY" -> Icons.Default.DirectionsBike
                                "IN_PROGRESS" -> Icons.Default.Build
                                else -> Icons.Default.Schedule
                            },
                            contentDescription = null,
                            tint = when (b.bookingStatus) {
                                "COMPLETED" -> EmeraldDark
                                "ON_THE_WAY", "ARRIVED", "IN_PROGRESS" -> PrimaryBlueDark
                                else -> AmberWarning
                            },
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "CURRENT STATUS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate500
                            )
                            Text(
                                text = b.bookingStatus ?: "ASSIGNED",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                        }
                    }
                }

                // 2. Customer & Address Details
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Customer Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = b.customerName ?: "Customer",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = b.address ?: "Address not provided",
                            fontSize = 14.sp,
                            color = Slate600,
                            lineHeight = 20.sp
                        )

                        if (!b.locality.isNullOrBlank()) {
                            Text(
                                text = "Locality: ${b.locality}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = PrimaryBlue,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Buttons: Call & Maps
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val phone = b.customerPhone?.replace("[^0-9+]".toRegex(), "") ?: ""
                                    if (phone.isNotBlank()) {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                        context.startActivity(intent)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Call")
                            }

                            Button(
                                onClick = {
                                    val lat = b.lat
                                    val lng = b.lng
                                    val uri = if (lat != null && lng != null) {
                                        Uri.parse("google.navigation:q=$lat,$lng&mode=d")
                                    } else {
                                        val query = Uri.encode("${b.address}, ${b.locality}, Bhilai")
                                        Uri.parse("geo:0,0?q=$query")
                                    }
                                    val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                                        setPackage("com.google.android.apps.maps")
                                    }
                                    try {
                                        context.startActivity(mapIntent)
                                    } catch (_: Exception) {
                                        // Fallback browser intent
                                        val webUri = if (lat != null && lng != null) {
                                            Uri.parse("https://www.google.com/maps?q=$lat,$lng")
                                        } else {
                                            Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode("${b.address}, ${b.locality}")}")
                                        }
                                        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldOnline)
                            ) {
                                Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Navigate")
                            }
                        }
                    }
                }

                // 3. Service & Pricing Breakdown
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Service Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = b.serviceName ?: "Electrical Repair",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate900
                        )

                        if (!b.issueDescription.isNullOrBlank()) {
                            Text(
                                text = "Issue: ${b.issueDescription}",
                                fontSize = 13.sp,
                                color = Slate600,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Slate200)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Service Charge", color = Slate600, fontSize = 14.sp)
                            Text("₹${b.totalAmount}", color = Slate900, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }

                        if ((b.partsAmount ?: 0) > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Spare Parts Added", color = Slate600, fontSize = 14.sp)
                                Text("₹${b.partsAmount}", color = Slate900, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Customer Bill", fontWeight = FontWeight.Bold, color = Slate900, fontSize = 16.sp)
                            Text("₹${b.finalAmount}", fontWeight = FontWeight.Bold, color = EmeraldDark, fontSize = 18.sp)
                        }
                    }
                }

                // 4. Lifecycle Actions (Next Step)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Next Action",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        when (b.bookingStatus) {
                            "TECHNICIAN_ASSIGNED" -> {
                                Button(
                                    onClick = {
                                        actionLoading = true
                                        scope.launch {
                                            try {
                                                apiService.updateBookingStatus(token, bookingId, UpdateStatusRequest(status = "ON_THE_WAY"))
                                                loadBooking()
                                            } catch (_: Exception) {}
                                            actionLoading = false
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                    enabled = !actionLoading
                                ) {
                                    Text("I Am On The Way ➔", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }

                            "ON_THE_WAY" -> {
                                Button(
                                    onClick = {
                                        actionLoading = true
                                        scope.launch {
                                            try {
                                                apiService.updateBookingStatus(token, bookingId, UpdateStatusRequest(status = "ARRIVED"))
                                                loadBooking()
                                            } catch (_: Exception) {}
                                            actionLoading = false
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldOnline),
                                    enabled = !actionLoading
                                ) {
                                    Text("Mark as Arrived at Doorstep 🏠", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }

                            "ARRIVED" -> {
                                Text(
                                    text = "Ask customer for 4-digit Job Start OTP:",
                                    fontSize = 13.sp,
                                    color = Slate600
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = otpInput,
                                    onValueChange = { if (it.length <= 4) otpInput = it },
                                    label = { Text("4-Digit OTP") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        actionLoading = true
                                        scope.launch {
                                            try {
                                                apiService.updateBookingStatus(token, bookingId, UpdateStatusRequest(status = "IN_PROGRESS", otp = otpInput))
                                                loadBooking()
                                            } catch (_: Exception) {}
                                            actionLoading = false
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                    enabled = !actionLoading
                                ) {
                                    Text("Start Work ⚡", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }

                            "IN_PROGRESS" -> {
                                Text(
                                    text = "Any extra parts replaced? (Leave 0 if none)",
                                    fontSize = 13.sp,
                                    color = Slate600
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = partsCostInput,
                                    onValueChange = { partsCostInput = it },
                                    label = { Text("Parts / Material Cost (₹)") },
                                    prefix = { Text("₹") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = {
                                        actionLoading = true
                                        scope.launch {
                                            try {
                                                val parts = partsCostInput.toIntOrNull() ?: 0
                                                apiService.updateBookingStatus(
                                                    token,
                                                    bookingId,
                                                    UpdateStatusRequest(status = "COMPLETED", partsCost = parts)
                                                )
                                                loadBooking()
                                            } catch (_: Exception) {}
                                            actionLoading = false
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldOnline),
                                    enabled = !actionLoading
                                ) {
                                    Text("Complete Job & Collect Cash/UPI", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }

                            "COMPLETED" -> {
                                Text(
                                    text = "✅ Job Successfully Completed! Payout has been added to your balance.",
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldDark,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
