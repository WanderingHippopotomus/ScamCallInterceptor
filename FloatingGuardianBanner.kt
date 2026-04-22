package com.tejas.grandparentguardian

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The user interface for the floating banner that appears during a call.
 * Uses a compact design to avoid blocking the dialer significantly.
 */
@Composable
fun FloatingGuardianBanner(
    callerNumber: String,
    transcript: String,
    riskLevel: Float,
    alertMessage: String,
    onDismiss: () -> Unit
) {
    val animatedRisk by animateFloatAsState(targetValue = riskLevel, label = "risk")
    
    val riskColor = when {
        riskLevel >= 0.7f -> Color(0xFFD32F2F)
        riskLevel >= 0.4f -> Color(0xFFF57C00)
        else -> Color(0xFF388E3C)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🛡️ Guardian Active", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(callerNumber, fontSize = 12.sp, color = Color.Gray)
                }
                
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Text("✕", fontSize = 14.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Risk Meter
            LinearProgressIndicator(
                progress = animatedRisk,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = riskColor,
                trackColor = Color(0xFFE0E0E0)
            )

            // Alert Box (Visible only when detection occurs)
            AnimatedVisibility(visible = alertMessage.isNotEmpty()) {
                Card(
                    modifier = Modifier.padding(top = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("⚠️", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(alertMessage, color = Color(0xFFB71C1C), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Live Transcript (Compact)
            Text(
                text = if (transcript.length > 100) "..." + transcript.takeLast(100) else transcript,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = Color.DarkGray,
                maxLines = 2
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Tip: Put call on Speaker for protection",
                fontSize = 10.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
