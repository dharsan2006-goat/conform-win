package com.bitchat.android.mesh

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * LoRa radio configuration — sent from phone to ESP32 via BLE.
 *
 * The ESP32 receives this as a special config packet (first byte = 0xCF)
 * and applies it to the SX1278 LoRa module.
 *
 * IMPORTANT: Different countries require different frequencies:
 *   - Asia (India, China, etc.):  433 MHz
 *   - Europe (EU):                868 MHz
 *   - Americas (US, Brazil, etc.): 915 MHz
 *   Using the wrong frequency may be illegal in your country.
 */
data class LoRaConfig(
    val frequency: LoRaFrequency = LoRaFrequency.MHZ_433,
    val spreadingFactor: Int = 7,       // 6-12 (higher = longer range, slower)
    val bandwidth: LoRaBandwidth = LoRaBandwidth.BW_125,
    val codingRate: Int = 5,            // 5-8 (denominator of 4/x)
    val txPower: Int = 20,              // 2-20 dBm
    val syncWord: Int = 0x12,           // Private sync word
    val enableCRC: Boolean = true
) {
    /**
     * Encode config into binary for BLE transmission to ESP32.
     * Format: [0xCF][frequency 4B][SF 1B][BW 1B][CR 1B][TXP 1B][SW 1B][CRC 1B]
     * Total: 11 bytes
     */
    fun toBytes(): ByteArray {
        val freq = frequency.hz
        return byteArrayOf(
            0xCF.toByte(),                         // Config command marker
            ((freq shr 24) and 0xFF).toByte(),     // Frequency byte 3
            ((freq shr 16) and 0xFF).toByte(),     // Frequency byte 2
            ((freq shr 8) and 0xFF).toByte(),      // Frequency byte 1
            (freq and 0xFF).toByte(),              // Frequency byte 0
            spreadingFactor.toByte(),              // Spreading factor
            bandwidth.code.toByte(),               // Bandwidth code
            codingRate.toByte(),                   // Coding rate
            txPower.toByte(),                      // TX power
            syncWord.toByte(),                     // Sync word
            if (enableCRC) 1 else 0                // CRC flag
        )
    }
}

/**
 * LoRa frequency bands — legal bands vary by country
 */
enum class LoRaFrequency(val hz: Int, val label: String, val region: String) {
    MHZ_433(433000000, "433 MHz", "Asia / Africa"),
    MHZ_868(868000000, "868 MHz", "Europe / India"),
    MHZ_915(915000000, "915 MHz", "Americas / Oceania");

    companion object {
        fun fromHz(hz: Int): LoRaFrequency = entries.minByOrNull {
            kotlin.math.abs(it.hz - hz)
        } ?: MHZ_433
    }
}

/**
 * LoRa bandwidth options
 */
enum class LoRaBandwidth(val khz: Double, val code: Int, val label: String) {
    BW_7_8(7.8, 0, "7.8 kHz"),
    BW_10_4(10.4, 1, "10.4 kHz"),
    BW_15_6(15.6, 2, "15.6 kHz"),
    BW_20_8(20.8, 3, "20.8 kHz"),
    BW_31_25(31.25, 4, "31.25 kHz"),
    BW_41_7(41.7, 5, "41.7 kHz"),
    BW_62_5(62.5, 6, "62.5 kHz"),
    BW_125(125.0, 7, "125 kHz"),
    BW_250(250.0, 8, "250 kHz"),
    BW_500(500.0, 9, "500 kHz");

    companion object {
        fun fromCode(code: Int): LoRaBandwidth = entries.find { it.code == code } ?: BW_125
    }
}

/**
 * Persists LoRa configuration to SharedPreferences
 */
object LoRaConfigPreferences {
    private const val TAG = "LoRaConfigPrefs"
    private const val PREFS_NAME = "meshwave_lora_config"
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun save(config: LoRaConfig) {
        prefs?.edit()?.apply {
            putInt("frequency_hz", config.frequency.hz)
            putInt("spreading_factor", config.spreadingFactor)
            putInt("bandwidth_code", config.bandwidth.code)
            putInt("coding_rate", config.codingRate)
            putInt("tx_power", config.txPower)
            putInt("sync_word", config.syncWord)
            putBoolean("enable_crc", config.enableCRC)
            apply()
        }
        Log.d(TAG, "Saved LoRa config: $config")
    }

    fun load(): LoRaConfig {
        val p = prefs ?: return LoRaConfig()
        return LoRaConfig(
            frequency = LoRaFrequency.fromHz(p.getInt("frequency_hz", 433000000)),
            spreadingFactor = p.getInt("spreading_factor", 7),
            bandwidth = LoRaBandwidth.fromCode(p.getInt("bandwidth_code", 7)),
            codingRate = p.getInt("coding_rate", 5),
            txPower = p.getInt("tx_power", 20),
            syncWord = p.getInt("sync_word", 0x12),
            enableCRC = p.getBoolean("enable_crc", true)
        )
    }
}
