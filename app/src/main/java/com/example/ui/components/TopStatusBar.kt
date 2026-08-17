package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BridgeStatus
import com.example.model.MtkChipInfo
import com.example.model.TransportType
import com.example.protocol.TargetPhoneState
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning

@Composable
fun TopStatusBar(
    bridgeStatus: BridgeStatus,
    targetPhoneState: TargetPhoneState,
    chipInfo: MtkChipInfo,
    isDryRun: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: App branding & Simulation Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = "Chip Bridge",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "MTK BROM Flash Bridge",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "ESP32-S3 Hardware Controller",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isDryRun) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = StatusWarning.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(StatusWarning)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "DRY-RUN SIMULATION",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusWarning
                            )
                        }
                    }
                }
            }

            // Row 2: Live Status Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // USB OTG Status Badge
                StatusPill(
                    icon = when (bridgeStatus.transportType) {
                        TransportType.USB_OTG_DIRECT -> Icons.Default.Usb
                        TransportType.SIMULATION -> Icons.Default.PlayArrow
                    },
                    title = "USB OTG",
                    value = if (bridgeStatus.isConnected) "Host Active" else "Scanning...",
                    statusColor = if (bridgeStatus.isConnected) StatusSuccess else StatusWarning,
                    modifier = Modifier.weight(1f)
                )

                // Target Phone USB Status Badge
                StatusPill(
                    icon = Icons.Default.Smartphone,
                    title = "Port Status",
                    value = when (targetPhoneState) {
                        is TargetPhoneState.Connected -> targetPhoneState.mode.label
                        is TargetPhoneState.RequestingPermission -> "Perm (${targetPhoneState.mode.label})"
                        is TargetPhoneState.Error -> "Port Error"
                        is TargetPhoneState.Disconnected -> if (isDryRun) "BROM (Sim)" else "Unplugged"
                    },
                    statusColor = when (targetPhoneState) {
                        is TargetPhoneState.Connected -> when (targetPhoneState.mode) {
                            com.example.protocol.UsbDeviceMode.BROM -> StatusSuccess
                            com.example.protocol.UsbDeviceMode.PRELOADER -> Color(0xFF3B82F6)
                            com.example.protocol.UsbDeviceMode.FASTBOOT -> Color(0xFFF59E0B)
                            com.example.protocol.UsbDeviceMode.ADB -> Color(0xFF06B6D4)
                            com.example.protocol.UsbDeviceMode.MTP -> Color(0xFFA855F7)
                            com.example.protocol.UsbDeviceMode.CDC_SERIAL -> Color(0xFF10B981)
                            else -> StatusSuccess
                        }
                        is TargetPhoneState.RequestingPermission -> StatusWarning
                        is TargetPhoneState.Error -> StatusError
                        is TargetPhoneState.Disconnected -> if (isDryRun) StatusSuccess else Color(0xFF64748B)
                    },
                    modifier = Modifier.weight(1f)
                )

                // Chip ID Badge
                StatusPill(
                    icon = Icons.Default.Power,
                    title = "Chipset",
                    value = chipInfo.chipIdHex.substringBefore(" "),
                    statusColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun StatusPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    statusColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }
        }
    }
}
