package com.example.bambunet

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

// ============================================================================
// 1. PALET WARNA TEMA TELEKOMUNIKASI: CYBER TEAL, ELECTRIC CYAN & TITANIUM
// ============================================================================
val TelecomSlate = Color(0xFF0F172A)      // Slate Gelap Instrumen
val DeepOceanBlue = Color(0xFF0369A1)     // Biru Spektrum Frekuensi
val ElectricCyan = Color(0xFF06B6D4)      // Cyan Sinyal Aktif
val CyberTeal = Color(0xFF0D9488)         // Teal Utama
val IceTitaniumBg = Color(0xFFF0F9FF)     // Background Cerah Sejuk
val SoftCyanBorder = Color(0xFFBAE6FD)    // Garis Tepi Kartu
val SignalExcellent = Color(0xFF10B981)   // Hijau (-30 s/d -60 dBm)
val SignalGood = Color(0xFFF59E0B)        // Kuning/Amber (-61 s/d -75 dBm)
val SignalPoor = Color(0xFFEF4444)        // Merah (<-75 dBm)
val PureWhite = Color(0xFFFFFFFF)

val BambuNetColorScheme = lightColorScheme(
    primary = DeepOceanBlue,
    onPrimary = PureWhite,
    primaryContainer = SoftCyanBorder,
    onPrimaryContainer = TelecomSlate,
    secondary = CyberTeal,
    onSecondary = PureWhite,
    secondaryContainer = IceTitaniumBg,
    onSecondaryContainer = TelecomSlate,
    background = IceTitaniumBg,
    onBackground = TelecomSlate,
    surface = PureWhite,
    onSurface = TelecomSlate
)

// ============================================================================
// 2. MODEL DATA ACCESS POINT (WI-FI), PING, PORT & SURVEY FINGERPRINTING
// ============================================================================
data class WifiApNode(
    val ssid: String,
    val bssid: String,
    val rssiDbm: Int,
    val freqMhz: Int,
    val channel: Int,
    val band: String,         // "2.4 GHz", "5 GHz", "6 GHz"
    val bandwidthMhz: Int,    // 20, 40, 80, 160 MHz
    val security: String,     // WPA3, WPA2, Open
    val standard: String,     // Wi-Fi 4/5/6
    val vendorHint: String,
    val distanceMeters: Double
)

data class RssiFingerprintRecord(
    val id: Long = System.currentTimeMillis(),
    val locationLabel: String, // Misal: "Lab Jaringan - Titik (2,4)"
    val posX: String,
    val posY: String,
    val topBssid: String,
    val topSsid: String,
    val rssiDbm: Int,
    val totalApDetected: Int,
    val allApVector: String,   // JSON/String vektor pasangan BSSID:RSSI untuk dataset ML
    val timestamp: String
)

data class PortScanItem(
    val port: Int,
    val serviceName: String,
    val isOpen: Boolean,
    val responseTimeMs: Long
)

// ============================================================================
// 3. ENGINE RUMUS FISIKA TELEKOMUNIKASI & JARINGAN (RF CALCULATOR)
// ============================================================================
object TelecomMath {

    // Konversi Frekuensi (MHz) ke Nomor Channel Standar IEEE 802.11
    fun freqToChannel(freqMhz: Int): Int {
        return when {
            freqMhz == 2484 -> 14
            freqMhz in 2412..2472 -> (freqMhz - 2407) / 5
            freqMhz in 5170..5825 -> (freqMhz - 5000) / 5
            freqMhz in 5955..7115 -> (freqMhz - 5950) / 5
            else -> 1
        }
    }

    fun getBandName(freqMhz: Int): String {
        return when {
            freqMhz in 2400..2500 -> "2.4 GHz"
            freqMhz in 4900..5900 -> "5 GHz"
            freqMhz > 5900 -> "6 GHz"
            else -> "Sub-GHz"
        }
    }

    // Rumus Estimasi Jarak Fisik Router (Log-Distance Path Loss / FSPL)
    // d = 10 ^ ((27.55 - (20 * log10(f_MHz)) + |RSSI| - EnvLoss) / (10 * n))
    fun estimateDistanceMeters(rssiDbm: Int, freqMhz: Int, pathLossExp: Double = 2.7): Double {
        val f = freqMhz.coerceAtLeast(2412).toDouble()
        val exp = (27.55 - (20.0 * log10(f)) + kotlin.math.abs(rssiDbm) - 8.0) / (10.0 * pathLossExp)
        val dist = 10.0.pow(exp)
        return ((dist * 10.0).roundToInt() / 10.0).coerceIn(0.5, 250.0)
    }

    // Konversi Kualitas Sinyal dBm ke Persentase (0 - 100%)
    fun dbmToQualityPercent(rssiDbm: Int): Int {
        return when {
            rssiDbm >= -50 -> 100
            rssiDbm <= -100 -> 0
            else -> 2 * (rssiDbm + 100)
        }
    }

    // Konversi Daya dBm ke miliWatt (mW) -> P(mW) = 10^(dBm / 10)
    fun dbmToMilliwatt(dbm: Double): Double {
        return 10.0.pow(dbm / 10.0)
    }

    // Rumus Free Space Path Loss (FSPL dalam satuan dB)
    // FSPL(dB) = 20*log10(d_km) + 20*log10(f_MHz) + 32.44
    fun calculateFsplDb(distanceKm: Double, freqMhz: Double): Double {
        if (distanceKm <= 0.0 || freqMhz <= 0.0) return 0.0
        return 20.0 * log10(distanceKm) + 20.0 * log10(freqMhz) + 32.44
    }

    // Rumus Jari-Jari Zona Fresnel ke-1 (First Fresnel Zone Radius dalam Meter)
    // F1 (m) = 17.32 * sqrt(d_km / (4 * f_GHz))
    fun calculateFresnelRadiusMeters(distanceKm: Double, freqGhz: Double): Double {
        if (distanceKm <= 0.0 || freqGhz <= 0.0) return 0.0
        return 17.32 * sqrt(distanceKm / (4.0 * freqGhz))
    }

    // Deteksi Vendor OUI Sederhana dari 3 Byte Pertama MAC Address (BSSID)
    fun guessVendorFromBssid(bssid: String): String {
        val prefix = bssid.uppercase().take(8)
        return when {
            prefix.startsWith("00:1A") || prefix.startsWith("F8:1A") || prefix.startsWith("EC:08") -> "TP-Link"
            prefix.startsWith("E4:8D") || prefix.startsWith("4C:5E") || prefix.startsWith("D4:CA") -> "MikroTik"
            prefix.startsWith("78:8A") || prefix.startsWith("F0:9F") || prefix.startsWith("24:A4") -> "Ubiquiti (UniFi)"
            prefix.startsWith("00:E0") || prefix.startsWith("48:8F") || prefix.startsWith("88:28") -> "Huawei / ZTE"
            prefix.startsWith("00:1E") || prefix.startsWith("00:24") || prefix.startsWith("C0:25") -> "Cisco / Linksys"
            else -> "IEEE 802.11 AP"
        }
    }

    // Kalkulator Subnet IPv4 (CIDR /8 s/d /32)
    fun calculateSubnetInfo(ipAddress: String, cidrPrefix: Int): Map<String, String> {
        val prefix = cidrPrefix.coerceIn(8, 32)
        val maskInt = (-0x1 shl (32 - prefix))
        val maskStr = listOf(
            (maskInt ushr 24) and 0xFF,
            (maskInt ushr 16) and 0xFF,
            (maskInt ushr 8) and 0xFF,
            maskInt and 0xFF
        ).joinToString(".")

        val totalHosts = if (prefix >= 31) 0L else (1L shl (32 - prefix)) - 2L
        val wildcardStr = listOf(
            ((maskInt.inv()) ushr 24) and 0xFF,
            ((maskInt.inv()) ushr 16) and 0xFF,
            ((maskInt.inv()) ushr 8) and 0xFF,
            (maskInt.inv()) and 0xFF
        ).joinToString(".")

        return mapOf(
            "CIDR Notation" to "$ipAddress/$prefix",
            "Subnet Mask" to maskStr,
            "Wildcard Mask" to wildcardStr,
            "Usable Hosts" to "$totalHosts Perangkat Aktif"
        )
    }
}