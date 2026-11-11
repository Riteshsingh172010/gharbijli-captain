package me.gharbijli.partner.dispatch

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import me.gharbijli.partner.data.DispatchRequest
import me.gharbijli.partner.ui.theme.*

@Composable
fun IncomingBroadcastDialog(
    request: DispatchRequest,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    var secondsLeft by remember { mutableStateOf(request.remainingSeconds.coerceAtLeast(10)) }

    LaunchedEffect(request.dispatchId) {
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft -= 1
        }
        onReject()
    }

    val progress by animateFloatAsState(
        targetValue = secondsLeft / 90f,
        label = "countdown"
    )

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(AmberLight)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = AmberWarning,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "NEW BROADCAST JOB",
                        color = AmberWarning,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Circular Timer
                Box(
                    modifier = Modifier.size(90.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        color = if (secondsLeft > 20) PrimaryBlue else RedOffline,
                        strokeWidth = 6.dp,
                        trackColor = Slate100
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${secondsLeft}s",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (secondsLeft > 20) Slate900 else RedOffline
                        )
                        Text(
                            text = "Hurry",
                            fontSize = 10.sp,
                            color = Slate500,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Payout Badge
                val displayPayout = if (request.payoutAmount > 0) request.payoutAmount else 50
                Text(
                    text = "₹$displayPayout",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldDark
                )
                Text(
                    text = "Direct Captain Payout",
                    fontSize = 13.sp,
                    color = Slate600
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Slate200)
                Spacer(modifier = Modifier.height(16.dp))

                // Service Details
                Text(
                    text = request.serviceName ?: "Electrical Repair",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Slate500,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${request.locality ?: "Bhilai"} (~${request.distanceKm} km)",
                        color = Slate700,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (!request.customerAddress.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = request.customerAddress,
                        color = Slate500,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Button(
                    onClick = onAccept,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldOnline)
                ) {
                    Text(
                        text = "ACCEPT BOOKING",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate600)
                ) {
                    Text("Decline / Pass", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
