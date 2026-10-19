package com.example.bambunet

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WifiRadarScreen(
    apList: MutableList<WifiApNode>,
    onRequestPermissionAndScan: () -> Unit
) {
    val context = LocalContext.current
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0 = Daftar AP, 1 = Kurva Channel, 2 = Signal Radar
    var selectedBandFilter by remember { mutableStateOf("Semua") }
    var pathLossExponent by remember { mutableDoubleStateOf(2.7) }
    var trackedTargetAp by remember { mutableStateOf<WifiApNode?>(null) }

    LaunchedEffect(Unit) {
        if (apList.isEmpty()) {
            apList.addAll(scanOrSimulateWifi(context, pathLossExponent))
        }
    }

    val filteredAps = remember(apList.toList(), selectedBandFilter) {
        apList.filter { ap ->
            selectedBandFilter == "Semua" || ap.band.equals(selectedBandFilter, ignoreCase = true)
        }.sortedByDescending { it.rssiDbm }
    }

    val best24ChannelInfo = remember(apList.toList()) {
        val mainChannels = listOf(1, 6, 11)
        val bestCh = mainChannels.minByOrNull { ch ->
            apList.filter { it.band == "2.4 GHz" && kotlin.math.abs(it.channel - ch) <= 2 }
                .sumOf { 100 + it.rssiDbm }
        } ?: 11
        "Kanal $bestCh (Minim Interferensi CCI)"
    }

    // SELURUH LAYAR DIBUNGKUS LAZYCOLUMN AGAR TIDAK PERNAH TERPOTONG DI LAYAR KECIL
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. BANNER ATAS RF SPECTRUM (DENGAN LAZYROW AGAR TOMBOL BETON TEBAL TIDAK RUSAK/KEPOTONG)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TelecomSlate),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.5.dp, ElectricCyan),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "📡 RF Spectrum & AP Scanner",
                                color = PureWhite,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "🏆 Rekomendasi 2.4GHz: $best24ChannelInfo",
                                color = SignalExcellent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                onRequestPermissionAndScan()
                                val fresh = scanOrSimulateWifi(context, pathLossExponent)
                                apList.clear()
                                apList.addAll(fresh)
                                Toast.makeText(context, "Spektrum diperbarui (${fresh.size} AP)", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = TelecomSlate, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SCAN RF", color = TelecomSlate, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                        }
                    }

                    Text(
                        text = "👉 Geser Kalibrasi Model Jarak (Path Loss Exponent):",
                        color = SoftCyanBorder,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    // LazyRow membuat tombol Beton Tebal tampil utuh & bisa digeser
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val models = listOf(
                            2.0 to "LOS Outdoor (n=2.0)",
                            2.7 to "Ruang / Lab (n=2.7)",
                            3.5 to "Dinding Beton Tebal (n=3.5)"
                        )
                        items(models) { (expVal, label) ->
                            val isSelected = pathLossExponent == expVal
                            Surface(
                                color = if (isSelected) CyberTeal else Color(0xFF1E293B),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (isSelected) ElectricCyan else SoftCyanBorder.copy(alpha = 0.4f)),
                                modifier = Modifier.clickable {
                                    pathLossExponent = expVal
                                    val updated = apList.map {
                                        it.copy(distanceMeters = TelecomMath.estimateDistanceMeters(it.rssiDbm, it.freqMhz, expVal))
                                    }
                                    apList.clear()
                                    apList.addAll(updated)
                                }
                            ) {
                                Text(
                                    text = label,
                                    color = PureWhite,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. SUB-MENU 3 MODE VISUALISASI
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple(0, "Daftar AP (${filteredAps.size})", Icons.Default.Wifi),
                    Triple(1, "Kurva Channel", Icons.Default.Timeline),
                    Triple(2, "Signal Radar", Icons.Default.Radar)
                ).forEach { (idx, title, icon) ->
                    val isSelected = selectedSubTab == idx
                    Button(
                        onClick = { selectedSubTab = idx },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) DeepOceanBlue else PureWhite
                        ),
                        border = BorderStroke(1.5.dp, if (isSelected) ElectricCyan else SoftCyanBorder),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                    ) {
                        Icon(icon, contentDescription = null, tint = if (isSelected) ElectricCyan else DeepOceanBlue, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(title, color = if (isSelected) PureWhite else TelecomSlate, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                    }
                }
            }
        }

        // 3. FILTER PITA FREKUENSI (SEMUA, 2.4 GHz, 5 GHz, 6 GHz)
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(listOf("Semua", "2.4 GHz", "5 GHz", "6 GHz")) { band ->
                    val isSelected = selectedBandFilter == band
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedBandFilter = band },
                        label = { Text(band, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberTeal,
                            selectedLabelColor = PureWhite,
                            containerColor = PureWhite
                        )
                    )
                }
            }
        }

        // 4. ISI KONTEN SESUAI MODE YANG DIPILIH (LANGSUNG TAMPIL DI BAWAHNYA & BISA DI-SCROLL)
        when (selectedSubTab) {
            0 -> {
                items(filteredAps, key = { it.bssid + it.freqMhz }) { ap ->
                    ApInspectorCardItem(
                        ap = ap,
                        onSelectForRadar = { selectedAp ->
                            trackedTargetAp = selectedAp
                            selectedSubTab = 2
                        }
                    )
                }
            }
            1 -> {
                item {
                    ChannelParabolaSpectrumGraph(apList = filteredAps)
                }
            }
            2 -> {
                item {
                    SignalHunterGeigerMeter(
                        apList = filteredAps,
                        selectedTarget = trackedTargetAp ?: filteredAps.firstOrNull(),
                        onSelectTarget = { trackedTargetAp = it }
                    )
                }
            }
        }
    }
}

@Composable
fun ApInspectorCardItem(
    ap: WifiApNode,
    onSelectForRadar: (WifiApNode) -> Unit
) {
    val signalColor = when {
        ap.rssiDbm >= -60 -> SignalExcellent
        ap.rssiDbm >= -75 -> SignalGood
        else -> SignalPoor
    }
    val qualityPct = TelecomMath.dbmToQualityPercent(ap.rssiDbm)

    Card(
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, SoftCyanBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectForRadar(ap) }
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = ap.ssid.ifBlank { "<Hidden SSID>" },
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = TelecomSlate,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(color = IceTitaniumBg, shape = RoundedCornerShape(6.dp), border = BorderStroke(1.dp, DeepOceanBlue)) {
                            Text(
                                text = "${ap.band} • CH ${ap.channel}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = DeepOceanBlue,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "BSSID: ${ap.bssid.uppercase()} • ${ap.vendorHint}",
                        fontSize = 11.sp,
                        color = Color.DarkGray
                    )
                }

                Surface(
                    color = signalColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, signalColor)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("${ap.rssiDbm} dBm", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = signalColor)
                        Text("± ${ap.distanceMeters} m", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TelecomSlate)
                    }
                }
            }

            LinearProgressIndicator(
                progress = { qualityPct / 100f },
                color = signalColor,
                trackColor = IceTitaniumBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Freq: ${ap.freqMhz} MHz (${ap.bandwidthMhz}MHz)", fontSize = 11.sp, color = TelecomSlate, fontWeight = FontWeight.SemiBold)
                Text("${ap.standard} • ${ap.security}", fontSize = 11.sp, color = CyberTeal, fontWeight = FontWeight.Bold)
                Text("Radar ➔", fontSize = 11.sp, color = DeepOceanBlue, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
fun ChannelParabolaSpectrumGraph(apList: List<WifiApNode>) {
    val curveColors = listOf(
        Color(0xFF06B6D4),
        Color(0xFF10B981),
        Color(0xFFF59E0B),
        Color(0xFFEF4444),
        Color(0xFF8B5CF6),
        Color(0xFFEC4899)
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = TelecomSlate),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(2.dp, ElectricCyan),
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "📈 Grafik Spektrum Kanal & Tumpang Tindih Frekuensi",
                color = PureWhite,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp
            )
            Text(
                text = "Kurva parabola yang saling bertumpuk menandakan Co-Channel Interference (CCI).",
                color = SoftCyanBorder,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF09101D), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                val w = size.width
                val h = size.height
                val bottomY = h - 30f

                val dbmLevels = listOf(-30, -50, -70, -90)
                dbmLevels.forEach { dbm ->
                    val ratio = (dbm + 100f) / 70f
                    val y = bottomY - (ratio * (bottomY - 20f))
                    drawLine(
                        color = Color.White.copy(alpha = 0.12f),
                        start = Offset(45f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.5f
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        "${dbm}",
                        4f,
                        y + 5f,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.LTGRAY
                            textSize = 22f
                        }
                    )
                }

                for (ch in 1..14) {
                    val x = 55f + ((ch - 1) / 13f) * (w - 75f)
                    drawContext.canvas.nativeCanvas.drawText(
                        "$ch",
                        x - 8f,
                        h - 4f,
                        android.graphics.Paint().apply {
                            color = if (ch == 1 || ch == 6 || ch == 11) android.graphics.Color.CYAN else android.graphics.Color.GRAY
                            textSize = 22f
                            isFakeBoldText = (ch == 1 || ch == 6 || ch == 11)
                        }
                    )
                }

                apList.forEachIndexed { idx, ap ->
                    val col = curveColors[idx % curveColors.size]
                    val mappedCh = if (ap.channel in 1..14) ap.channel else ((ap.channel % 13) + 1)
                    val centerX = 55f + ((mappedCh - 1) / 13f) * (w - 75f)
                    val spreadWidth = (w - 75f) * (if (ap.bandwidthMhz >= 40) 0.25f else 0.15f)

                    val signalRatio = ((ap.rssiDbm.coerceIn(-98, -30) + 100f) / 70f)
                    val peakY = bottomY - (signalRatio * (bottomY - 25f))

                    val path = Path().apply {
                        moveTo((centerX - spreadWidth).coerceAtLeast(45f), bottomY)
                        quadraticBezierTo(
                            centerX,
                            peakY - (bottomY - peakY) * 0.45f,
                            (centerX + spreadWidth).coerceAtMost(w),
                            bottomY
                        )
                    }

                    drawPath(path = path, color = col.copy(alpha = 0.22f))
                    drawPath(path = path, color = col, style = Stroke(width = 4f, cap = StrokeCap.Round))

                    drawContext.canvas.nativeCanvas.drawText(
                        "${ap.ssid.take(9)} (${ap.rssiDbm})",
                        centerX - 42f,
                        (peakY - 8f).coerceAtLeast(24f),
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = 22f
                            isFakeBoldText = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SignalHunterGeigerMeter(
    apList: List<WifiApNode>,
    selectedTarget: WifiApNode?,
    onSelectTarget: (WifiApNode) -> Unit
) {
    val target = selectedTarget ?: apList.firstOrNull()
    val rssi = target?.rssiDbm ?: -80
    val quality = TelecomMath.dbmToQualityPercent(rssi)
    val mwPower = TelecomMath.dbmToMilliwatt(rssi.toDouble())

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(apList) { ap ->
                val isSelected = ap.bssid == target?.bssid
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectTarget(ap) },
                    label = { Text("${ap.ssid} (${ap.rssiDbm} dBm)", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(2.dp, DeepOceanBlue),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🎯 Pelacak Posisi Fisik Access Point", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = DeepOceanBlue)
                Text(target?.let { "${it.ssid} • BSSID: ${it.bssid}" } ?: "Pilih AP di atas", fontSize = 12.sp, color = Color.Gray)

                Spacer(modifier = Modifier.height(14.dp))

                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(190.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeW = 24f
                        drawArc(
                            color = Color(0xFFE2E8F0),
                            startAngle = 135f,
                            sweepAngle = 270f,
                            useCenter = false,
                            style = Stroke(width = strokeW, cap = StrokeCap.Round),
                            size = Size(size.width, size.height)
                        )
                        val sweep = (quality / 100f) * 270f
                        val arcColor = when {
                            rssi >= -60 -> SignalExcellent
                            rssi >= -75 -> SignalGood
                            else -> SignalPoor
                        }
                        drawArc(
                            color = arcColor,
                            startAngle = 135f,
                            sweepAngle = sweep,
                            useCenter = false,
                            style = Stroke(width = strokeW, cap = StrokeCap.Round),
                            size = Size(size.width, size.height)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$rssi dBm", fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, color = TelecomSlate)
                        Text("Kualitas: $quality%", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CyberTeal)
                        Text("Jarak: ± ${target?.distanceMeters ?: 0.0} m", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = DeepOceanBlue)
                    }
                }

                Surface(color = IceTitaniumBg, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceAround) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Frekuensi", fontSize = 11.sp, color = Color.Gray)
                            Text("${target?.freqMhz ?: 2412} MHz", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Daya (mW)", fontSize = 11.sp, color = Color.Gray)
                            Text(String.format("%.2e mW", mwPower), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Kanal", fontSize = 11.sp, color = Color.Gray)
                            Text("CH ${target?.channel ?: 1}", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
fun scanOrSimulateWifi(context: Context, pathLossExp: Double): List<WifiApNode> {
    return try {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiManager.startScan()
        val results = wifiManager.scanResults
        if (!results.isNullOrEmpty()) {
            results.map { res ->
                val freq = res.frequency
                val rssi = res.level
                val bw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    when (res.channelWidth) {
                        1 -> 40
                        2 -> 80
                        3, 4 -> 160
                        else -> 20
                    }
                } else 20

                val sec = when {
                    res.capabilities.contains("WPA3") -> "WPA3-SAE"
                    res.capabilities.contains("WPA2") -> "WPA2-PSK"
                    res.capabilities.contains("WEP") -> "WEP"
                    else -> "Open"
                }

                WifiApNode(
                    ssid = res.SSID.ifBlank { "Hidden_Network" },
                    bssid = res.BSSID ?: "00:11:22:33:44:55",
                    rssiDbm = rssi,
                    freqMhz = freq,
                    channel = TelecomMath.freqToChannel(freq),
                    band = TelecomMath.getBandName(freq),
                    bandwidthMhz = bw,
                    security = sec,
                    standard = if (freq > 5000) "Wi-Fi 6 (802.11ax)" else "Wi-Fi 4/5 (802.11n/ac)",
                    vendorHint = TelecomMath.guessVendorFromBssid(res.BSSID ?: ""),
                    distanceMeters = TelecomMath.estimateDistanceMeters(rssi, freq, pathLossExp)
                )
            }
        } else {
            generateSimulatedLabAps(pathLossExp)
        }
    } catch (e: Exception) {
        generateSimulatedLabAps(pathLossExp)
    }
}

fun generateSimulatedLabAps(pathLossExp: Double): List<WifiApNode> {
    val rawSamples = listOf(
        Triple("PandaKode_Core_5G", "78:8A:20:44:10:A1", Pair(-46, 5745)),
        Triple("Lab_Telekomunikasi_AP1", "E4:8D:8C:12:99:B2", Pair(-54, 2412)),
        Triple("PCR_Academic_WiFi", "24:A4:3C:88:71:C3", Pair(-63, 5180)),
        Triple("Gedung_Utama_Lobby", "00:1A:2B:55:60:D4", Pair(-68, 2437)),
        Triple("IoT_MQTT_Gateway", "F8:1A:67:31:09:E5", Pair(-72, 2437)),
        Triple("Riset_Indoor_Node_A", "4C:5E:0C:91:22:F6", Pair(-79, 2462)),
        Triple("Guest_Hotspot_24G", "00:E0:4C:19:84:A7", Pair(-85, 2412))
    )
    return rawSamples.map { (ssid, bssid, rf) ->
        val (baseRssi, freq) = rf
        val jitter = (-2..2).random()
        val finalRssi = (baseRssi + jitter).coerceIn(-95, -35)
        WifiApNode(
            ssid = ssid,
            bssid = bssid,
            rssiDbm = finalRssi,
            freqMhz = freq,
            channel = TelecomMath.freqToChannel(freq),
            band = TelecomMath.getBandName(freq),
            bandwidthMhz = if (freq > 5000) 80 else 20,
            security = if (freq > 5000) "WPA3/WPA2" else "WPA2-PSK",
            standard = if (freq > 5000) "Wi-Fi 6 (802.11ax)" else "Wi-Fi 4 (802.11n)",
            vendorHint = TelecomMath.guessVendorFromBssid(bssid),
            distanceMeters = TelecomMath.estimateDistanceMeters(finalRssi, freq, pathLossExp)
        )
    }
}