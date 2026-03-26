package com.bitchat.android.mesh

import android.bluetooth.BluetoothDevice
import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks discovered and connected ESP32 LoRa bridge nodes.
 * Separate from BluetoothConnectionTracker which handles mesh phone peers.
 */
class ESP32DeviceTracker {

    companion object {
        private const val TAG = "ESP32DeviceTracker"
    }

    /**
     * Represents a discovered or connected ESP32 bridge
     */
    data class ESP32Device(
        val device: BluetoothDevice,
        val rssi: Int,
        val discoveredAt: Long = System.currentTimeMillis(),
        var isConnected: Boolean = false,
        var lastSeen: Long = System.currentTimeMillis()
    )

    // All discovered ESP32 devices (by MAC address)
    private val discoveredDevices = ConcurrentHashMap<String, ESP32Device>()

    // Currently connected ESP32 device address
    @Volatile
    var connectedDeviceAddress: String? = null
        private set

    /**
     * Record a discovered ESP32 device from BLE scan
     */
    fun onDeviceDiscovered(device: BluetoothDevice, rssi: Int) {
        val addr = device.address
        val existing = discoveredDevices[addr]
        if (existing != null) {
            // Update RSSI and last seen
            discoveredDevices[addr] = existing.copy(rssi = rssi, lastSeen = System.currentTimeMillis())
        } else {
            discoveredDevices[addr] = ESP32Device(device, rssi)
            Log.d(TAG, "New ESP32 bridge discovered: $addr (RSSI: $rssi)")
        }
    }

    /**
     * Mark an ESP32 device as connected
     */
    fun onDeviceConnected(address: String) {
        connectedDeviceAddress = address
        discoveredDevices[address]?.let {
            discoveredDevices[address] = it.copy(isConnected = true)
        }
        Log.d(TAG, "ESP32 bridge connected: $address")
    }

    /**
     * Mark an ESP32 device as disconnected
     */
    fun onDeviceDisconnected(address: String) {
        if (connectedDeviceAddress == address) {
            connectedDeviceAddress = null
        }
        discoveredDevices[address]?.let {
            discoveredDevices[address] = it.copy(isConnected = false)
        }
        Log.d(TAG, "ESP32 bridge disconnected: $address")
    }

    /**
     * Get the best (strongest RSSI) unconnected ESP32 device
     */
    fun getBestDevice(): ESP32Device? {
        return discoveredDevices.values
            .filter { !it.isConnected }
            .maxByOrNull { it.rssi }
    }

    /**
     * Check if any ESP32 bridge is currently connected
     */
    fun hasConnectedBridge(): Boolean = connectedDeviceAddress != null

    /**
     * Get count of discovered ESP32 devices
     */
    fun getDiscoveredCount(): Int = discoveredDevices.size

    /**
     * Get all discovered devices
     */
    fun getAllDevices(): List<ESP32Device> = discoveredDevices.values.toList()

    /**
     * Remove stale devices not seen in the last 60 seconds
     */
    fun cleanupStaleDevices() {
        val cutoff = System.currentTimeMillis() - 60_000L
        val stale = discoveredDevices.entries.filter {
            it.value.lastSeen < cutoff && !it.value.isConnected
        }
        stale.forEach { entry ->
            discoveredDevices.remove(entry.key)
            Log.d(TAG, "Removed stale ESP32: ${entry.key}")
        }
    }
}
