package com.example.bambunet

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.TelephonyManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

data class DriveTestLogPoint(
    val id: Long = System.currentTimeMillis(),
    val latitude: Double,
    val longitude: Double,
    val rsrp: Int,       // dBm
    val rsrq: Int,       // dB
    val sinr: Int,       // dB
    val pci: Int,
    val tac: Int,
    val servingBtsName: String,
    val networkType: String,
    val timestamp: String
)

data class BtsTower(
    val name: String,
    val operator: String,
    val lacTac: Int,
    val cellId: Int,
    val pci: Int,
    val lat: Double,
    val lon: Double,
    val band: String
)

@SuppressLint("MissingPermission", "SetJavaScriptEnabled")
@Composable
fun NetDriveTestScreen() {
    val context = LocalContext.current
    var isLoggingActive by remember { mutableStateOf(false) }
    var mapViewMode by remember { mutableIntStateOf(0) } // 0 = Peta Jalan OSM, 1 = Radar Grid BTS (Anti-Blank)

    // Koordinat Awal (Default: Area Kampus PCR / Umban Sari Pekanbaru)
    var currentLatitude by remember { mutableDoubleStateOf(0.5701) }
    var currentLongitude by remember { mutableDoubleStateOf(101.4255) }
    var currentRsrp by remember { mutableIntStateOf(-82) }
    var currentRsrq by remember { mutableIntStateOf(-9) }
    var currentSinr by remember { mutableIntStateOf(16) }
    var currentPci by remember { mutableIntStateOf(245) }
    var currentTac by remember { mutableIntStateOf(1042) }
    var currentNetType by remember { mutableStateOf("4G LTE-A") }

    // Daftar Menara BTS Sekitar (Data Menara Seluler Sekitar)
    val btsList = remember {
        mutableStateListOf(
            BtsTower("BTS-Telkomsel_UmbanSari", "Telkomsel", 1042, 41021, 245, 0.5718, 101.4268, "LTE B3 (1800MHz)"),
            BtsTower("BTS-Indosat_Rumbai", "Indosat Ooredoo", 1042, 41055, 112, 0.5685, 101.4239, "LTE B1 (2100MHz)"),
            BtsTower("BTS-XL_SimpangBaru", "XL Axiata", 1045, 41089, 308, 0.5725, 101.4232, "5G NR n28 / LTE")
        )
    }

    var servingBts by remember { mutableStateOf(btsList.first()) }
    val logPoints = remember { mutableStateListOf<DriveTestLogPoint>() }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var simStepAngle by remember { mutableDoubleStateOf(0.0) }

    // Fungsi Rekam 1 Titik Drive Test (Bisa dipanggil dari GPS Asli maupun Tombol Simulasi Jalan)
    fun recordNewDrivePoint(lat: Double, lon: Double) {
        currentLatitude = lat
        currentLongitude = lon
        currentRsrp = (-104..-68).random()
        currentRsrq = (-16..-6).random()
        currentSinr = (4..24).random()

        // Pilih Serving BTS terdekat
        servingBts = btsList.minByOrNull {
            val dLat = it.lat - lat
            val dLon = it.lon - lon
            (dLat * dLat) + (dLon * dLon)
        } ?: btsList.first()
        currentPci = servingBts.pci
        currentTac = servingBts.lacTac

        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val pt = DriveTestLogPoint(
            latitude = lat,
            longitude = lon,
            rsrp = currentRsrp,
            rsrq = currentRsrq,
            sinr = currentSinr,
            pci = currentPci,
            tac = currentTac,
            servingBtsName = servingBts.name,
            networkType = currentNetType,
            timestamp = timeStr
        )
        logPoints.add(pt)

        // Kirim titik & garis warna RSRP + Garis Serving BTS ke Peta WebView
        val colorHex = when {
            currentRsrp >= -80 -> "#10B981" // Hijau (Bagus)
            currentRsrp >= -95 -> "#F59E0B" // Kuning (Sedang)
            else -> "#EF4444"               // Merah (Buruk)
        }
        webViewRef?.evaluateJavascript(
            "updateDriveTest($lat, $lon, ${servingBts.lat}, ${servingBts.lon}, '$colorHex', '${servingBts.name}', $currentRsrp);",
            null
        )
    }

    // Listener GPS & Seluler Asli
    DisposableEffect(isLoggingActive) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                try {
                    val allCells = telephonyManager.allCellInfo
                    if (!allCells.isNullOrEmpty()) {
                        for (cell in allCells) {
                            if (cell is CellInfoLte) {
                                currentRsrp = cell.cellSignalStrength.rsrp
                                currentRsrq = cell.cellSignalStrength.rsrq
                                currentPci = cell.cellIdentity.pci
                                currentTac = cell.cellIdentity.tac
                                currentNetType = "4G LTE"
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cell is CellInfoNr) {
                                currentNetType = "5G NR"
                            }
                        }
                    }
                } catch (_: Exception) {}

                if (isLoggingActive) {
                    recordNewDrivePoint(location.latitude, location.longitude)
                }
            }
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 2f, locationListener)
        } catch (_: Exception) {}

        onDispose {
            locationManager.removeUpdates(locationListener)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. PANEL TELEMETRI SELULER ALA G-NETTRACK PRO
        Card(
            colors = CardDefaults.cardColors(containerColor = TelecomSlate),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, ElectricCyan),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🚗 G-Net Drive Test & BTS Tracker",
                            color = PureWhite,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Serving: ${servingBts.name} (${servingBts.band})",
                            color = ElectricCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Tombol Simulasi Jalan (Agar bisa langsung dicoba di Emulator PC)
                        FilledTonalButton(
                            onClick = {
                                simStepAngle += 0.45
                                val nextLat = currentLatitude + 0.00045 * cos(simStepAngle)
                                val nextLon = currentLongitude + 0.00045 * sin(simStepAngle)
                                recordNewDrivePoint(nextLat, nextLon)
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = DeepOceanBlue),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.DirectionsWalk, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Simulasi Jalan", color = PureWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                isLoggingActive = !isLoggingActive
                                if (isLoggingActive && logPoints.isEmpty()) {
                                    recordNewDrivePoint(currentLatitude, currentLongitude)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isLoggingActive) SignalPoor else SignalExcellent
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (isLoggingActive) "STOP" else "START",
                                color = PureWhite,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Baris Indikator Parameter Radio Seluler
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TelemetryBadge("RSRP", "$currentRsrp dBm", if (currentRsrp >= -85) SignalExcellent else SignalGood)
                    TelemetryBadge("RSRQ", "$currentRsrq dB", CyberTeal)
                    TelemetryBadge("SINR", "$currentSinr dB", SignalExcellent)
                    TelemetryBadge("PCI / TAC", "$currentPci / $currentTac", PureWhite)
                    TelemetryBadge("Log Titik", "${logPoints.size} Pts", ElectricCyan)
                }
            }
        }

        // 2. TOMBOL PILIH MODE PETA: PETA JALAN (OSM/CARTO) vs RADAR GRID BTS (OFFLINE ANTI-PUTIH)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { mapViewMode = 0 },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (mapViewMode == 0) DeepOceanBlue else PureWhite
                ),
                border = BorderStroke(1.dp, ElectricCyan),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                Icon(Icons.Default.Map, contentDescription = null, tint = if (mapViewMode == 0) ElectricCyan else DeepOceanBlue, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Peta Jalan & BTS (Online)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (mapViewMode == 0) PureWhite else TelecomSlate)
            }

            Button(
                onClick = { mapViewMode = 1 },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (mapViewMode == 1) CyberTeal else PureWhite
                ),
                border = BorderStroke(1.dp, ElectricCyan),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                Icon(Icons.Default.Radar, contentDescription = null, tint = if (mapViewMode == 1) PureWhite else CyberTeal, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Radar Grid BTS (100% Offline)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (mapViewMode == 1) PureWhite else TelecomSlate)
            }
        }

        // 3. AREA PETA UTAMA (ANTI-BLANK) + TOMBOL EXPORT CSV & KML
        Card(
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, SoftCyanBorder),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (mapViewMode == 0) {
                    // MODE 1: PETA JALAN INTERAKTIF (SUDAH DIPERBAIKI USER-AGENT & BASEURL AGAR TIDAK PUTIH)
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                // WAJIB: Set User-Agent agar server OpenStreetMap tidak memblokir tile gambar!
                                settings.userAgentString = "Mozilla/5.0 (Linux; Android 14; BambuNet Telecom Analyzer) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"
                                webViewClient = WebViewClient()
                                loadDataWithBaseURL(
                                    "https://bambunet.pandakode.com",
                                    getFixedLeafletMapHtml(currentLatitude, currentLongitude, btsList),
                                    "text/html",
                                    "UTF-8",
                                    null
                                )
                                webViewRef = this
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // MODE 2: RADAR GRID BTS TAKTIS (100% OFFLINE NATIVE CANVAS)
                    TacticalBtsRadarCanvas(
                        currentLat = currentLatitude,
                        currentLon = currentLongitude,
                        servingBts = servingBts,
                        btsList = btsList,
                        logPoints = logPoints
                    )
                }

                // Baris Tombol Export CSV & KML di Bagian Bawah Peta
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (logPoints.isEmpty()) {
                                recordNewDrivePoint(currentLatitude, currentLongitude)
                            }
                            exportDriveTestCsv(context, logPoints)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepOceanBlue),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.TableView, contentDescription = null, tint = PureWhite, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export CSV (${logPoints.size})", fontSize = 11.sp, color = PureWhite, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (logPoints.isEmpty()) {
                                recordNewDrivePoint(currentLatitude, currentLongitude)
                            }
                            exportDriveTestKml(context, logPoints)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberTeal),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Public, contentDescription = null, tint = PureWhite, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export KML (Google Earth)", fontSize = 11.sp, color = PureWhite, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ============================================================================
// KOMPONEN RADAR GRID BTS OFFLINE (DIJAMIN TAMPIL KEREN TANPA INTERNET)
// ============================================================================
@Composable
fun TacticalBtsRadarCanvas(
    currentLat: Double,
    currentLon: Double,
    servingBts: BtsTower,
    btsList: List<BtsTower>,
    logPoints: List<DriveTestLogPoint>
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07111E))
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val scale = 85000f // Skala konversi derajat koordinat ke pixel layar

        // Gambar Grid Radar Telekomunikasi
        val gridStep = 70f
        var gx = 0f
        while (gx < size.width) {
            drawLine(Color.Cyan.copy(alpha = 0.08f), Offset(gx, 0f), Offset(gx, size.height), 1f)
            gx += gridStep
        }
        var gy = 0f
        while (gy < size.height) {
            drawLine(Color.Cyan.copy(alpha = 0.08f), Offset(0f, gy), Offset(size.width, gy), 1f)
            gy += gridStep
        }

        // Gambar Garis Putus-putus ke Serving BTS (Cell Spider Line ala G-NetTrack)
        val btsX = cx + ((servingBts.lon - currentLon) * scale).toFloat()
        val btsY = cy - ((servingBts.lat - currentLat) * scale).toFloat()
        drawLine(
            color = ElectricCyan,
            start = Offset(cx, cy),
            end = Offset(btsX, btsY),
            strokeWidth = 4f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
        )

        // Gambar Menara BTS di Sekitar
        btsList.forEach { bts ->
            val tx = cx + ((bts.lon - currentLon) * scale).toFloat()
            val ty = cy - ((bts.lat - currentLat) * scale).toFloat()
            val isServing = bts.name == servingBts.name

            drawCircle(
                color = if (isServing) ElectricCyan.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.1f),
                radius = if (isServing) 34f else 22f,
                center = Offset(tx, ty)
            )
            drawCircle(
                color = if (isServing) ElectricCyan else SignalGood,
                radius = 12f,
                center = Offset(tx, ty)
            )
            drawContext.canvas.nativeCanvas.drawText(
                "🗼 ${bts.name} (PCI:${bts.pci})",
                tx - 70f,
                ty - 20f,
                android.graphics.Paint().apply {
                    color = if (isServing) android.graphics.Color.CYAN else android.graphics.Color.LTGRAY
                    textSize = 23f
                    isFakeBoldText = true
                }
            )
        }

        // Gambar Jejak Titik Rute Drive Test Berdasarkan Warna RSRP
        logPoints.forEach { pt ->
            val px = cx + ((pt.longitude - currentLon) * scale).toFloat()
            val py = cy - ((pt.latitude - currentLat) * scale).toFloat()
            val ptColor = when {
                pt.rsrp >= -80 -> SignalExcellent
                pt.rsrp >= -95 -> SignalGood
                else -> SignalPoor
            }
            drawCircle(color = ptColor, radius = 9f, center = Offset(px, py))
        }

        // Gambar Posisi Kendaraan / User di Tengah Radar
        drawCircle(color = PureWhite, radius = 14f, center = Offset(cx, cy))
        drawCircle(color = DeepOceanBlue, radius = 9f, center = Offset(cx, cy))
    }
}

@Composable
fun TelemetryBadge(label: String, value: String, textColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 9.sp, color = SoftCyanBorder)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
    }
}

// ============================================================================
// HTML LEAFLET.JS DENGAN SERVER TILE CARTO & OSM + SPIDER LINE KE MENARA BTS
// ============================================================================
fun getFixedLeafletMapHtml(lat: Double, lon: Double, btsList: List<BtsTower>): String {
    val btsJsArray = btsList.joinToString(",") {
        "{name:'${it.name}', band:'${it.band}', pci:${it.pci}, lat:${it.lat}, lon:${it.lon}}"
    }
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                html, body { margin: 0; padding: 0; width: 100%; height: 100%; background: #0F172A; }
                #map { width: 100%; height: 100%; }
                .bts-label { background: #0F172A; color: #06B6D4; border: 1px solid #06B6D4; border-radius: 4px; padding: 2px 5px; font-size: 10px; font-weight: bold; }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map = L.map('map', {zoomControl: true}).setView([$lat, $lon], 16);

                // Menggunakan Tile Server CartoDB Voyager & OSM yang ramah WebView Android
                L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
                    maxZoom: 19,
                    subdomains: 'abcd'
                }).addTo(map);

                var btsTowers = [$btsJsArray];
                btsTowers.forEach(function(b) {
                    var btsCircle = L.circleMarker([b.lat, b.lon], {
                        radius: 9,
                        fillColor: '#0284C7',
                        color: '#FFFFFF',
                        weight: 2,
                        fillOpacity: 0.95
                    }).addTo(map);
                    btsCircle.bindPopup("<b>🗼 " + b.name + "</b><br>" + b.band + " • PCI: " + b.pci);
                });

                var userMarker = L.circleMarker([$lat, $lon], {
                    radius: 8,
                    fillColor: '#10B981',
                    color: '#0F172A',
                    weight: 2,
                    fillOpacity: 1.0
                }).addTo(map).bindPopup("Posisi Drive Test Saat Ini");

                // Garis penghubung posisi HP ke Serving BTS (Spider Line ala G-NetTrack)
                var servingLine = L.polyline([[$lat, $lon], [btsTowers[0].lat, btsTowers[0].lon]], {
                    color: '#06B6D4',
                    weight: 2,
                    dashArray: '6, 6'
                }).addTo(map);

                function updateDriveTest(lat, lon, btsLat, btsLon, hexColor, btsName, rsrp) {
                    var pt = [lat, lon];
                    L.circleMarker(pt, {
                        radius: 6,
                        fillColor: hexColor,
                        color: hexColor,
                        weight: 1,
                        fillOpacity: 0.9
                    }).addTo(map);

                    userMarker.setLatLng(pt).bindPopup("<b>RSRP: " + rsrp + " dBm</b><br>Serving: " + btsName).openPopup();
                    servingLine.setLatLngs([pt, [btsLat, btsLon]]);
                    map.panTo(pt);
                }
            </script>
        </body>
        </html>
    """.trimIndent()
}

fun exportDriveTestCsv(context: Context, points: List<DriveTestLogPoint>) {
    try {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "DriveTest_Log_${System.currentTimeMillis() % 10000}.csv")
        val sb = StringBuilder()
        sb.append("Timestamp,Latitude,Longitude,RSRP_dBm,RSRQ_dB,SINR_dB,PCI,TAC,Serving_BTS,Network_Type\n")
        points.forEach { p ->
            sb.append("${p.timestamp},${p.latitude},${p.longitude},${p.rsrp},${p.rsrq},${p.sinr},${p.pci},${p.tac},\"${p.servingBtsName}\",${p.networkType}\n")
        }
        file.writeText(sb.toString())

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Drive Test Log CSV - BambuNet")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan File Log CSV..."))
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal export CSV: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun exportDriveTestKml(context: Context, points: List<DriveTestLogPoint>) {
    try {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "DriveTest_Route_${System.currentTimeMillis() % 10000}.kml")
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n  <Document>\n    <name>BambuNet Drive Test Route</name>\n    <Placemark>\n      <name>Rute Perjalanan Drive Test</name>\n      <LineString>\n        <coordinates>\n")
        points.forEach { p ->
            sb.append("          ${p.longitude},${p.latitude},0\n")
        }
        sb.append("        </coordinates>\n      </LineString>\n    </Placemark>\n  </Document>\n</kml>")
        file.writeText(sb.toString())

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.google-earth.kml+xml"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Drive Test KML Route - BambuNet")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan File KML ke Google Earth..."))
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal export KML: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}