package com.bitchat.android.mesh

import android.util.Log

/**
 * Helper for splitting packets that exceed the LoRa max payload (255 bytes).
 * Uses the same fragment format as the ESP32 firmware:
 *   [4 bytes fragment ID][1 byte: high nibble=index, low nibble=total][data]
 *
 * Note: This is separate from the mesh FragmentManager which handles BLE MTU fragmentation.
 * LoRa fragmentation happens at the transport layer before sending to ESP32.
 */
object LoRaFragmentHelper {

    private const val TAG = "LoRaFragmentHelper"
    private const val HEADER_SIZE = ESP32BridgeConstants.LORA_FRAGMENT_HEADER_SIZE
    private const val MAX_DATA = ESP32BridgeConstants.LORA_MAX_DATA_PER_FRAGMENT
    private const val MAX_PACKET = ESP32BridgeConstants.LORA_MAX_PACKET_SIZE

    /**
     * Check if a packet needs LoRa-level fragmentation
     */
    fun needsFragmentation(data: ByteArray): Boolean {
        return data.size > MAX_PACKET
    }

    /**
     * Split a large packet into LoRa-sized fragments.
     * Each fragment has a 5-byte header: [4 bytes ID][1 byte index|total]
     *
     * The phone doesn't normally need to fragment — it sends the full bitchat
     * binary packet to ESP32 via BLE (which supports up to ~512 bytes).
     * The ESP32 firmware handles LoRa fragmentation internally.
     *
     * This helper exists as a safety net for very large packets.
     */
    fun fragment(data: ByteArray): List<ByteArray> {
        if (!needsFragmentation(data)) {
            return listOf(data)
        }

        val fragmentId = kotlin.random.Random.nextInt()
        val totalFragments = (data.size + MAX_DATA - 1) / MAX_DATA

        if (totalFragments > 15) {
            Log.e(TAG, "Packet too large for LoRa fragmentation: ${data.size} bytes ($totalFragments fragments)")
            return emptyList()
        }

        val fragments = mutableListOf<ByteArray>()
        for (i in 0 until totalFragments) {
            val offset = i * MAX_DATA
            val chunkSize = minOf(MAX_DATA, data.size - offset)

            val fragment = ByteArray(HEADER_SIZE + chunkSize)
            // Fragment ID (4 bytes, big-endian)
            fragment[0] = ((fragmentId shr 24) and 0xFF).toByte()
            fragment[1] = ((fragmentId shr 16) and 0xFF).toByte()
            fragment[2] = ((fragmentId shr 8) and 0xFF).toByte()
            fragment[3] = (fragmentId and 0xFF).toByte()
            // Index (high nibble) | Total (low nibble)
            fragment[4] = (((i and 0x0F) shl 4) or (totalFragments and 0x0F)).toByte()
            // Data
            System.arraycopy(data, offset, fragment, HEADER_SIZE, chunkSize)

            fragments.add(fragment)
        }

        Log.d(TAG, "Fragmented ${data.size} bytes into $totalFragments LoRa fragments")
        return fragments
    }
}
