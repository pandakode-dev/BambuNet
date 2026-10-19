package com.example.bambunet

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TelecomLabScreen(
    currentApList: List<WifiApNode>,
    onTriggerFreshScan: () -> Unit
) {
    val context = LocalContext.current
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0 = Survey RSSI Logger, 1 = Kalkulator RF & CIDR

    val surveyRecords = remember {
        mutableStateListOf<RssiFingerprintRecord>().apply {
            addAll(loadFingerprintsFromPrefs(context))
        }
    }

    fun syncSurveyStorage() {
        saveFingerprintsToPrefs(context, surveyRecords)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Tombol Switch Sub-Menu: RSSI Survey Logger vs RF Calculator Suite
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { selectedSubTab = 0 },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedSubTab == 0) DeepOceanBlue else PureWhite
                ),
                border = BorderStroke(1.5.dp, if (selectedSubTab == 0) ElectricCyan else SoftCyanBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.PinDrop,
                    contentDescription = null,
                    tint = if (selectedSubTab == 0) ElectricCyan else DeepOceanBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "RSSI Logger (${surveyRecords.size})",
                    color = if (selectedSubTab == 0) PureWhite else TelecomSlate,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp
                )
            }

            Button(
                onClick = { selectedSubTab = 1 },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedSubTab == 1) CyberTeal else PureWhite
                ),
                border = BorderStroke(1.5.dp, if (selectedSubTab == 1) ElectricCyan else SoftCyanBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Calculate,
                    contentDescription = null,
                    tint = if (selectedSubTab == 1) PureWhite else CyberTeal,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Kalkulator RF & IP",
                    color = if (selectedSubTab == 1) PureWhite else TelecomSlate,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp
                )
            }
        }

        if (selectedSubTab == 0) {
            RssiFingerprintingLoggerView(
                currentApList = currentApList,
                surveyRecords = surveyRecords,
                onTriggerFreshScan = onTriggerFreshScan,
                onAddRecord = { rec ->
                    surveyRecords.add(0, rec)
                    syncSurveyStorage()
                },
                onDeleteRecord = { rec ->
                    surveyRecords.remove(rec)
                    syncSurveyStorage()
                },
                onClearAll = {
                    surveyRecords.clear()
                    syncSurveyStorage()
                }
            )
        } else {
            RfAndSubnetCalculatorView()
        }
    }
}

// ============================================================================
// SUB-LAYAR 1: WI-FI RSSI FINGERPRINTING & SITE SURVEY LOGGER (EXPORT CSV)
// ============================================================================
@Composable
fun RssiFingerprintingLoggerView(
    currentApList: List<WifiApNode>,
    surveyRecords: List<RssiFingerprintRecord>,
    onTriggerFreshScan: () -> Unit,
    onAddRecord: (RssiFingerprintRecord) -> Unit,
    onDeleteRecord: (RssiFingerprintRecord) -> Unit,
    onClearAll: () -> Unit
) {
    val context = LocalContext.current
    var roomLabel by remember { mutableStateOf("Lab Telekomunikasi") }
    var coordX by remember { mutableStateOf("2.0") }
    var coordY by remember { mutableStateOf("4.5") }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TelecomSlate),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.5.dp, ElectricCyan),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "📍 Indoor Positioning (RSSI Fingerprinting)",
                                color = PureWhite,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Rekam vektor BSSID & RSSI pada titik koordinat (X,Y) untuk dataset riset / pemetaan gedung.",
                                color = SoftCyanBorder,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (surveyRecords.isEmpty()) {
                                    Toast.makeText(context, "Rekam minimal 1 titik ukur terlebih dahulu!", Toast.LENGTH_SHORT).show()
                                } else {
                                    exportFingerprintDatasetToCsv(context, surveyRecords)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SignalExcellent),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.TableView, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export Dataset (.CSV)", color = PureWhite, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        }

                        if (surveyRecords.isNotEmpty()) {
                            OutlinedButton(
                                onClick = onClearAll,
                                border = BorderStroke(1.dp, SignalPoor),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Reset", color = SignalPoor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Form Input Titik Koordinat Sampling
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, SoftCyanBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = roomLabel,
                        onValueChange = { roomLabel = it },
                        label = { Text("Nama Ruangan / Zona Pengukuran") },
                        leadingIcon = { Icon(Icons.Default.MeetingRoom, contentDescription = null, tint = DeepOceanBlue) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = coordX,
                            onValueChange = { coordX = it },
                            label = { Text("Koordinat X (m)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = coordY,
                            onValueChange = { coordY = it },
                            label = { Text("Koordinat Y (m)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Button(
                        onClick = {
                            onTriggerFreshScan()
                            val strongest = currentApList.maxByOrNull { it.rssiDbm }
                            val vectorStr = currentApList.joinToString("; ") { "${it.bssid}=${it.rssiDbm}" }
                            val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

                            val newRec = RssiFingerprintRecord(
                                locationLabel = roomLabel.ifBlank { "Titik Ukur" },
                                posX = coordX.ifBlank { "0.0" },
                                posY = coordY.ifBlank { "0.0" },
                                topBssid = strongest?.bssid ?: "00:00:00:00:00:00",
                                topSsid = strongest?.ssid ?: "Unknown",
                                rssiDbm = strongest?.rssiDbm ?: -90,
                                totalApDetected = currentApList.size,
                                allApVector = vectorStr,
                                timestamp = timeStr
                            )
                            onAddRecord(newRec)

                            // Auto-increment koordinat X (+1.0 m) agar survei titik berikutnya lebih cepat
                            val nextX = (coordX.toDoubleOrNull() ?: 0.0) + 1.0
                            coordX = String.format(Locale.US, "%.1f", nextX)
                            Toast.makeText(context, "Titik ($coordX, $coordY) berhasil direkam!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepOceanBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AddLocationAlt, contentDescription = null, tint = ElectricCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "REKAM SAMPEL TITIK INI (${currentApList.size} AP)",
                            color = PureWhite,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        // Daftar Titik Fingerprint yang Sudah Direkam
        items(surveyRecords, key = { it.id }) { rec ->
            Card(
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, SoftCyanBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(color = IceTitaniumBg, shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = "${rec.locationLabel} • Titik (${rec.posX}, ${rec.posY})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = DeepOceanBlue,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        IconButton(onClick = { onDeleteRecord(rec) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = SignalPoor, modifier = Modifier.size(18.dp))
                        }
                    }

                    Text(
                        text = "Dominan: ${rec.topSsid} (${rec.rssiDbm} dBm) • Total: ${rec.totalApDetected} AP",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TelecomSlate
                    )
                    Text(
                        text = "Vektor RSSI: ${rec.allApVector}",
                        fontSize = 11.sp,
                        color = Color.DarkGray,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

// ============================================================================
// SUB-LAYAR 2: KALKULATOR RF LINK BUDGET, FRESNEL ZONE, FSPL & SUBNETTING CIDR
// ============================================================================
@Composable
fun RfAndSubnetCalculatorView() {
    // State Link Budget & Fresnel Zone
    var txPowerDbm by remember { mutableStateOf("20.0") }       // 20 dBm (100 mW)
    var txGainDbi by remember { mutableStateOf("8.0") }         // Antena Pemancar 8 dBi
    var rxGainDbi by remember { mutableStateOf("8.0") }         // Antena Penerima 8 dBi
    var cableLossDb by remember { mutableStateOf("2.0") }       // Rugi Konektor/Kabel
    var distanceKm by remember { mutableStateOf("1.5") }        // Jarak Link 1.5 km
    var freqMhz by remember { mutableStateOf("5800") }          // Frekuensi 5.8 GHz

    val pTx = txPowerDbm.toDoubleOrNull() ?: 20.0
    val gTx = txGainDbi.toDoubleOrNull() ?: 0.0
    val gRx = rxGainDbi.toDoubleOrNull() ?: 0.0
    val lossCable = cableLossDb.toDoubleOrNull() ?: 0.0
    val dKm = (distanceKm.toDoubleOrNull() ?: 0.1).coerceAtLeast(0.01)
    val fMhz = (freqMhz.toDoubleOrNull() ?: 2412.0).coerceAtLeast(100.0)

    val fsplDb = TelecomMath.calculateFsplDb(dKm, fMhz)
    val eirpDbm = pTx + gTx - (lossCable / 2.0)
    val rxSignalDbm = pTx + gTx + gRx - lossCable - fsplDb
    val fresnelF1Meters = TelecomMath.calculateFresnelRadiusMeters(dKm, fMhz / 1000.0)
    val txMilliwatt = TelecomMath.dbmToMilliwatt(pTx)

    // State Kalkulator Subnetting IPv4 CIDR
    var ipInput by remember { mutableStateOf("192.168.10.1") }
    var cidrSlider by remember { mutableFloatStateOf(24f) }
    val subnetMap = remember(ipInput, cidrSlider) {
        TelecomMath.calculateSubnetInfo(ipInput, cidrSlider.toInt())
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. KALKULATOR RF LINK BUDGET, FSPL & ZONA FRESNEL
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(2.dp, DeepOceanBlue),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "📡 RF Link Budget, FSPL & Fresnel Zone Calculator",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = DeepOceanBlue
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = txPowerDbm,
                            onValueChange = { txPowerDbm = it },
                            label = { Text("Tx Power (dBm)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = freqMhz,
                            onValueChange = { freqMhz = it },
                            label = { Text("Frekuensi (MHz)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = txGainDbi,
                            onValueChange = { txGainDbi = it },
                            label = { Text("Gain Tx (dBi)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = rxGainDbi,
                            onValueChange = { rxGainDbi = it },
                            label = { Text("Gain Rx (dBi)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = distanceKm,
                            onValueChange = { distanceKm = it },
                            label = { Text("Jarak Link (km)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = cableLossDb,
                            onValueChange = { cableLossDb = it },
                            label = { Text("Cable Loss (dB)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    // Kotak Hasil Perhitungan RF
                    Surface(
                        color = TelecomSlate,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Received Power (Prx):", color = SoftCyanBorder, fontSize = 12.sp)
                                Text(
                                    text = String.format(Locale.US, "%.2f dBm", rxSignalDbm),
                                    color = if (rxSignalDbm >= -70) SignalExcellent else SignalGood,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Free Space Path Loss (FSPL):", color = SoftCyanBorder, fontSize = 12.sp)
                                Text(String.format(Locale.US, "%.2f dB", fsplDb), color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("EIRP & Daya Pancar:", color = SoftCyanBorder, fontSize = 12.sp)
                                Text(String.format(Locale.US, "%.1f dBm (%.1f mW)", eirpDbm, txMilliwatt), color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Radius Zona Fresnel ke-1 (F1):", color = SoftCyanBorder, fontSize = 12.sp)
                                Text(String.format(Locale.US, "%.2f Meter", fresnelF1Meters), color = ElectricCyan, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        // 2. KALKULATOR SUBNETTING IPv4 CIDR (/8 s.d. /32)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.5.dp, CyberTeal),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "🖧 IPv4 Subnet & CIDR Calculator",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = CyberTeal
                    )

                    OutlinedTextField(
                        value = ipInput,
                        onValueChange = { ipInput = it },
                        label = { Text("IPv4 Network / Host Address") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text(
                        text = "Prefix Length: /${cidrSlider.toInt()}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = TelecomSlate
                    )
                    Slider(
                        value = cidrSlider,
                        onValueChange = { cidrSlider = it },
                        valueRange = 8f..32f,
                        steps = 23,
                        colors = SliderDefaults.colors(thumbColor = DeepOceanBlue, activeTrackColor = CyberTeal)
                    )

                    Surface(
                        color = IceTitaniumBg,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            subnetMap.forEach { (k, v) ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(k, fontSize = 12.sp, color = Color.DarkGray, fontWeight = FontWeight.SemiBold)
                                    Text(v, fontSize = 12.sp, color = TelecomSlate, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// HELPER: SIMPAN & EXPORT DATASET RSSI FINGERPRINTING KE CSV
// ============================================================================
fun exportFingerprintDatasetToCsv(context: Context, list: List<RssiFingerprintRecord>) {
    try {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val csvFile = File(dir, "RSSI_Fingerprint_Dataset_${System.currentTimeMillis() % 10000}.csv")
        val sb = StringBuilder()
        sb.append("Timestamp,Room_Zone,Coord_X,Coord_Y,Dominant_SSID,Dominant_BSSID,Dominant_RSSI_dBm,Total_AP,BSSID_RSSI_Vector\n")
        list.forEach { r ->
            sb.append("${r.timestamp},\"${r.locationLabel}\",${r.posX},${r.posY},\"${r.topSsid}\",${r.topBssid},${r.rssiDbm},${r.totalApDetected},\"${r.allApVector}\"\n")
        }
        csvFile.writeText(sb.toString())

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", csvFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Dataset Wi-Fi RSSI Fingerprinting - BambuNet")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export Dataset CSV ke..."))
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal mengekspor CSV: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun saveFingerprintsToPrefs(context: Context, list: List<RssiFingerprintRecord>) {
    val arr = JSONArray()
    list.forEach { r ->
        arr.put(JSONObject().apply {
            put("id", r.id)
            put("locationLabel", r.locationLabel)
            put("posX", r.posX)
            put("posY", r.posY)
            put("topBssid", r.topBssid)
            put("topSsid", r.topSsid)
            put("rssiDbm", r.rssiDbm)
            put("totalApDetected", r.totalApDetected)
            put("allApVector", r.allApVector)
            put("timestamp", r.timestamp)
        })
    }
    context.getSharedPreferences("bambunet_prefs", Context.MODE_PRIVATE)
        .edit().putString("fingerprints_json", arr.toString()).apply()
}

fun loadFingerprintsFromPrefs(context: Context): List<RssiFingerprintRecord> {
    val raw = context.getSharedPreferences("bambunet_prefs", Context.MODE_PRIVATE)
        .getString("fingerprints_json", null) ?: return emptyList()
    return try {
        val arr = JSONArray(raw)
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            RssiFingerprintRecord(
                id = o.optLong("id"),
                locationLabel = o.optString("locationLabel"),
                posX = o.optString("posX"),
                posY = o.optString("posY"),
                topBssid = o.optString("topBssid"),
                topSsid = o.optString("topSsid"),
                rssiDbm = o.optInt("rssiDbm"),
                totalApDetected = o.optInt("totalApDetected"),
                allApVector = o.optString("allApVector"),
                timestamp = o.optString("timestamp")
            )
        }
    } catch (e: Exception) {
        emptyList()
    }
}