package com.example.bambunet

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = BambuNetColorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BambuNetMainApp()
                }
            }
        }
    }
}

@Composable
fun BambuNetMainApp() {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Spektrum RF, 1 = Ping & Port, 2 = Drive Test, 3 = Lab & Survey
    val apList = remember { mutableStateListOf<WifiApNode>() }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        val fresh = scanOrSimulateWifi(context, 2.7)
        apList.clear()
        apList.addAll(fresh)
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_PHONE_STATE
            )
        )
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(TelecomSlate, DeepOceanBlue, CyberTeal)
                        )
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = IceTitaniumBg,
                            border = BorderStroke(2.dp, ElectricCyan),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(2.dp)
                            ) {
                                Image(
                                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.logo_bambunet),
                                    contentDescription = "Logo BambuNet",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "BambuNet",
                                    color = PureWhite,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(color = ElectricCyan, shape = RoundedCornerShape(6.dp)) {
                                    Text(
                                        text = "TELECOM PRO",
                                        color = TelecomSlate,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "G-Net Drive Test • RF Spectrum • Subnet Suite",
                                color = SoftCyanBorder,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Surface(
                        color = TelecomSlate.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, ElectricCyan)
                    ) {
                        Text(
                            text = "${apList.size} AP Aktif",
                            color = ElectricCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(ElectricCyan)
                )
            }
        },
        bottomBar = {
            NavigationBar(containerColor = PureWhite, tonalElevation = 10.dp) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.WifiTethering, contentDescription = null) },
                    label = { Text("Spektrum RF", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PureWhite,
                        selectedTextColor = DeepOceanBlue,
                        indicatorColor = DeepOceanBlue
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Speed, contentDescription = null) },
                    label = { Text("Ping & Port", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PureWhite,
                        selectedTextColor = CyberTeal,
                        indicatorColor = CyberTeal
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.DirectionsCar, contentDescription = null) },
                    label = { Text("Drive Test", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PureWhite,
                        selectedTextColor = DeepOceanBlue,
                        indicatorColor = DeepOceanBlue
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Science, contentDescription = null) },
                    label = { Text("Lab & RF", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PureWhite,
                        selectedTextColor = CyberTeal,
                        indicatorColor = CyberTeal
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> WifiRadarScreen(
                    apList = apList,
                    onRequestPermissionAndScan = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                )
                1 -> NetworkDiagScreen()
                2 -> NetDriveTestScreen()
                3 -> TelecomLabScreen(
                    currentApList = apList,
                    onTriggerFreshScan = {
                        val fresh = scanOrSimulateWifi(context, 2.7)
                        apList.clear()
                        apList.addAll(fresh)
                    }
                )
            }
        }
    }
}