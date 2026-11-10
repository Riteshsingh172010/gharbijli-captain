package me.gharbijli.partner.earnings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import me.gharbijli.partner.data.*
import me.gharbijli.partner.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EarningsScreen(
    sessionManager: SessionManager,
    apiService: PartnerApiService,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val token = "Bearer ${sessionManager.getToken() ?: ""}"
    var earnings by remember { mutableStateOf<EarningsSummaryResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val res = apiService.getEarnings(token)
                if (res.isSuccessful) {
                    earnings = res.body()
                }
            } catch (_: Exception) {}
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Earnings & Payouts", fontWeight = FontWeight.Bold) },
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
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else {
            val summary = earnings ?: EarningsSummaryResponse(true)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Today's Big Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Slate900)
                    ) {
                        Column(modifier = Modifier.padding(22.dp)) {
                            Text("TODAY'S EARNINGS", color = Slate500, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "₹${summary.todayEarnings}",
                                color = EmeraldOnline,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${summary.jobsToday} Jobs completed today",
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // Summary Row (Week & Total)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("This Week", fontSize = 12.sp, color = Slate500)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("₹${summary.weekEarnings}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Slate900)
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("All Time Net", fontSize = 12.sp, color = Slate500)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("₹${summary.totalEarnings}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                            }
                        }
                    }
                }

                // Cash in Hand & Admin Commission Return Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "Cash Settlement (GharBijli Commission)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Cash Collected from Customers", color = Slate600, fontSize = 13.sp)
                                Text("₹${summary.totalCashCollected}", fontWeight = FontWeight.Bold, color = Slate900, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Your Net Earning", color = Slate600, fontSize = 13.sp)
                                Text("₹${summary.totalEarnings}", fontWeight = FontWeight.Bold, color = EmeraldDark, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = Slate200)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("To Return to Admin", fontWeight = FontWeight.Bold, color = RedDark, fontSize = 14.sp)
                                    Text("Platform commission to settle", color = Slate500, fontSize = 11.sp)
                                }
                                Text(
                                    text = "₹${summary.payableToAdmin}",
                                    fontWeight = FontWeight.Bold,
                                    color = RedDark,
                                    fontSize = 18.sp
                                )
                            }
                        }
                    }
                }

                // Recent Transactions Header
                item {
                    Text(
                        text = "Recent Completed Jobs",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                }

                val txs = summary.transactions ?: emptyList()
                if (txs.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("No recent payouts recorded yet.", color = Slate500, fontSize = 13.sp)
                            }
                        }
                    }
                } else {
                    items(txs.size) { idx ->
                        val tx = txs[idx]
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = tx.serviceName ?: "Electrical Service",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Slate900
                                    )
                                    Text(
                                        text = tx.bookingId ?: "",
                                        fontSize = 11.sp,
                                        color = Slate500
                                    )
                                }
                                Text(
                                    text = "+₹${tx.amount}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = EmeraldDark
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
