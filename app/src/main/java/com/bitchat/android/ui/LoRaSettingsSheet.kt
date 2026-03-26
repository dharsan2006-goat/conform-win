package com.bitchat.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bitchat.android.mesh.*

/**
 * LoRa Bridge Settings sheet — allows users to configure the ESP32 LoRa radio.
 * Accessible from the Debug Settings or About sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoRaSettingsSheet(
    meshService: BluetoothMeshService?,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val bridgeManager = meshService?.esp32BridgeManager
    val isConnected = bridgeManager?.isConnected() == true
    val discoveredCount = bridgeManager?.getDiscoveredCount() ?: 0

    // Load saved config
    var config by remember { mutableStateOf(LoRaConfigPreferences.load()) }
    var configSent by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Filled.CellTower,
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        "LoRa Bridge Settings",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onBackground
                    )
                }
            }

            // Connection status
            item {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isConnected) Color(0xFF1A3A1A) else colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                if (isConnected) "ESP32 Connected" else "No ESP32 Connected",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isConnected) Color(0xFF4CAF50) else colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                "$discoveredCount bridge(s) discovered",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isConnected) Color(0xFF4CAF50) else Color(0xFF666666)
                        ) {
                            Text(
                                if (isConnected) "ONLINE" else "OFFLINE",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Frequency selection
            item {
                SectionHeader("Frequency Band")
                Text(
                    "Select the legal frequency for your country",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LoRaFrequency.entries.forEach { freq ->
                        val selected = config.frequency == freq
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) colorScheme.primary.copy(alpha = 0.15f)
                            else colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { config = config.copy(frequency = freq) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        freq.label,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (selected) colorScheme.primary else colorScheme.onSurface
                                    )
                                    Text(
                                        freq.region,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                                if (selected) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Spreading Factor slider
            item {
                SectionHeader("Spreading Factor: ${config.spreadingFactor}")
                Text(
                    when (config.spreadingFactor) {
                        in 6..7 -> "Fast speed, shorter range (~2 km)"
                        in 8..9 -> "Balanced speed and range (~5 km)"
                        in 10..11 -> "Slow speed, long range (~8 km)"
                        else -> "Very slow, maximum range (~10+ km)"
                    },
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Slider(
                    value = config.spreadingFactor.toFloat(),
                    onValueChange = { config = config.copy(spreadingFactor = it.toInt()) },
                    valueRange = 6f..12f,
                    steps = 5,
                    colors = SliderDefaults.colors(
                        thumbColor = colorScheme.primary,
                        activeTrackColor = colorScheme.primary
                    )
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("SF6 (fast)", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = colorScheme.onSurface.copy(alpha = 0.4f))
                    Text("SF12 (far)", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = colorScheme.onSurface.copy(alpha = 0.4f))
                }
            }

            // Bandwidth selection
            item {
                SectionHeader("Bandwidth: ${config.bandwidth.label}")
                Text(
                    "Narrower = longer range but slower",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Spacer(Modifier.height(8.dp))
                val commonBandwidths = listOf(
                    LoRaBandwidth.BW_31_25,
                    LoRaBandwidth.BW_62_5,
                    LoRaBandwidth.BW_125,
                    LoRaBandwidth.BW_250,
                    LoRaBandwidth.BW_500
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    commonBandwidths.forEach { bw ->
                        val selected = config.bandwidth == bw
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (selected) colorScheme.primary.copy(alpha = 0.2f)
                            else colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { config = config.copy(bandwidth = bw) }
                        ) {
                            Text(
                                bw.label.replace(" kHz", ""),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = if (selected) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            // TX Power slider
            item {
                SectionHeader("TX Power: ${config.txPower} dBm")
                Text(
                    when (config.txPower) {
                        in 2..7 -> "Low power — saves battery, short range"
                        in 8..14 -> "Medium power — balanced"
                        else -> "High power — max range, uses more battery"
                    },
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Slider(
                    value = config.txPower.toFloat(),
                    onValueChange = { config = config.copy(txPower = it.toInt()) },
                    valueRange = 2f..20f,
                    steps = 17,
                    colors = SliderDefaults.colors(
                        thumbColor = colorScheme.primary,
                        activeTrackColor = colorScheme.primary
                    )
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("2 dBm", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = colorScheme.onSurface.copy(alpha = 0.4f))
                    Text("20 dBm", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = colorScheme.onSurface.copy(alpha = 0.4f))
                }
            }

            // Coding Rate
            item {
                SectionHeader("Coding Rate: 4/${config.codingRate}")
                Text(
                    "Higher = more error correction, slower",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (5..8).forEach { cr ->
                        val selected = config.codingRate == cr
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (selected) colorScheme.primary.copy(alpha = 0.2f)
                            else colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { config = config.copy(codingRate = cr) }
                        ) {
                            Text(
                                "4/$cr",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.padding(vertical = 10.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            // CRC toggle
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "CRC Check",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = colorScheme.onSurface
                        )
                        Text(
                            "Error detection (recommended ON)",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    Switch(
                        checked = config.enableCRC,
                        onCheckedChange = { config = config.copy(enableCRC = it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = colorScheme.primary)
                    )
                }
            }

            // Apply button
            item {
                // Debug Test Button
                Button(
                    onClick = {
                        bridgeManager?.forceSendTest()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isConnected,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red,
                        disabledContainerColor = colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "🔥 TEST ESP32 CONNECTION",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }
                
                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        LoRaConfigPreferences.save(config)
                        bridgeManager?.sendLoRaConfig(config)
                        configSent = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isConnected,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primary,
                        disabledContainerColor = colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (configSent) "Config Sent ✓" else if (isConnected) "Apply to ESP32" else "Connect ESP32 First",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                }

                if (!isConnected) {
                    Text(
                        "Settings are saved locally and will be sent when an ESP32 connects.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        fontFamily = FontFamily.Monospace,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
}
