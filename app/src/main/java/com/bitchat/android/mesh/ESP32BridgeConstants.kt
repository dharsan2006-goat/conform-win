package com.bitchat.android.mesh

import java.util.UUID

/**
 * Constants for ESP32 LoRa bridge nodes.
 * These UUIDs are DIFFERENT from the mesh phone UUIDs to allow
 * the app to distinguish ESP32 bridges from regular mesh phones.
 */
object ESP32BridgeConstants {
    // ESP32 bridge Service UUID — phones use F47B5E2D..., ESP32 uses this:
    val SERVICE_UUID: UUID = UUID.fromString("12345678-1234-1234-1234-1234567890ab")

    // Phone writes packets to ESP32 via this characteristic
    val WRITE_CHARACTERISTIC_UUID: UUID = UUID.fromString("12345678-1234-1234-1234-1234567890cd")

    // ESP32 notifies phone with LoRa-received packets via this characteristic
    val NOTIFY_CHARACTERISTIC_UUID: UUID = UUID.fromString("12345678-1234-1234-1234-1234567890ef")

    // Manufacturer data string that ESP32 advertises as fallback identification
    const val MANUFACTURER_DATA_STRING = "ESP32_LORA_NODE"

    // Connection parameters
    const val SCAN_INTERVAL_MS: Long = 10_000L       // Scan for ESP32 every 10 seconds
    const val RECONNECT_DELAY_MS: Long = 3_000L      // Wait 3s before reconnecting
    const val MAX_RECONNECT_ATTEMPTS: Int = 5         // Max auto-reconnect attempts
    const val CONNECTION_TIMEOUT_MS: Long = 10_000L   // Connection timeout

    // LoRa constraints
    const val LORA_MAX_PACKET_SIZE: Int = 255         // LoRa max payload
    const val LORA_FRAGMENT_HEADER_SIZE: Int = 5      // Fragment header overhead
    const val LORA_MAX_DATA_PER_FRAGMENT: Int = LORA_MAX_PACKET_SIZE - LORA_FRAGMENT_HEADER_SIZE
}
