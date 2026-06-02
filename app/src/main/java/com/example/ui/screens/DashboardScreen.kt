package com.example.ui.screens

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import com.example.data.model.DriveTrip
import com.example.data.model.MaintenanceLog
import com.example.ui.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: VehicleViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val language by viewModel.language.collectAsState()
    val carType by viewModel.carType.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val carManufacturer by viewModel.carManufacturer.collectAsState()
    val carModel by viewModel.carModel.collectAsState()
    
    val btState by viewModel.btState.collectAsState()
    val connectedDevice by viewModel.connectedDevice.collectAsState()
    
    // Core telemetry
    val speedKmh by viewModel.speedKmh.collectAsState()
    val rpm by viewModel.rpm.collectAsState()
    val engineTemp by viewModel.engineTemp.collectAsState()
    val throttlePos by viewModel.throttlePos.collectAsState()
    val batteryVolts by viewModel.batteryVolts.collectAsState()
    val fuelRate by viewModel.fuelRate.collectAsState()
    
    val tirePsiFL by viewModel.tirePsiFL.collectAsState()
    val tirePsiFR by viewModel.tirePsiFR.collectAsState()
    val tirePsiRL by viewModel.tirePsiRL.collectAsState()
    val tirePsiRR by viewModel.tirePsiRR.collectAsState()
    
    val minTireThresh by viewModel.minTirePressureThreshold.collectAsState()
    val minBatteryThresh by viewModel.minBatteryVoltsThreshold.collectAsState()
    val maxOilChangeDist by viewModel.maxOdometerBetweenOilChanges.collectAsState()
    
    // Offline stored metrics
    val maintenanceLogs by viewModel.maintenanceLogs.collectAsState()
    val driveTrips by viewModel.driveTrips.collectAsState()
    val totalMaintCost by viewModel.totalMaintenanceCost.collectAsState()
    val totalDistanceSum by viewModel.totalDistance.collectAsState()
    val totalFuelSum by viewModel.totalFuelConsumed.collectAsState()
    
    // Active track logging status
    val isTrackingTrip by viewModel.isTrackingTrip.collectAsState()
    val activeTripDistance by viewModel.activeTripDistance.collectAsState()
    val activeTripDuration by viewModel.activeTripDuration.collectAsState()
    val activeTripFuel by viewModel.activeTripFuel.collectAsState()
    val activeGpsPoints by viewModel.activeGpsPoints.collectAsState()
    
    // Cloud synchronization metrics
    val cloudSyncState by viewModel.cloudSyncState.collectAsState()
    val lastSyncedTime by viewModel.lastSyncedTime.collectAsState()
    
    // Diagnostic controls
    val troubleCodes by viewModel.troubleCodes.collectAsState()
    val isScanningDTC by viewModel.isScanningDTC.collectAsState()
    
    var activeTab by remember { mutableStateOf(0) }
    var showConnectionDialog by remember { mutableStateOf(false) }
    var showAddLogBottomSheet by remember { mutableStateOf(false) }

    var tempCarType by remember { mutableStateOf("sedan") }
    var tempManufacturer by remember { mutableStateOf("") }
    var tempModel by remember { mutableStateOf("") }

    // Helper trigger for showing Android System Notifications
    fun showSystemAlarmNotification(title: String, body: String) {
        val channelId = "obd_vehicle_faults"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Vehicle Alarms", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }
        
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
            
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    // Trigger notification if values fall below threshold
    LaunchedEffect(batteryVolts) {
        if (batteryVolts < minBatteryThresh && btState == BtState.CONNECTED) {
            showSystemAlarmNotification(
                LocaleStrings.get("low_battery_alert", language),
                "Battery charge dropped under standard thresholds: ${String.format("%.1f", batteryVolts)}V"
            )
        }
    }
    LaunchedEffect(tirePsiFL, tirePsiFR, tirePsiRL, tirePsiRR) {
        if ((tirePsiFL < minTireThresh || tirePsiFR < minTireThresh || tirePsiRL < minTireThresh || tirePsiRR < minTireThresh) && btState == BtState.CONNECTED) {
            showSystemAlarmNotification(
                LocaleStrings.get("low_tire_alert", language),
                "Check vehicle inflating pressures! Front Left: $tirePsiFL bar, Rear Right: $tirePsiRR bar"
            )
        }
    }

    // --- MAIN RENDER ROOT ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CockpitBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // AESTHETIC HIGH HEADER BANNER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CockpitSurface)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        val carBrand = carManufacturer ?: ""
                        val carModelName = carModel ?: ""
                        val carTypeLabel = when (carType) {
                            "sedan" -> LocaleStrings.get("sedan", language)
                            "hatchback" -> LocaleStrings.get("hatchback", language)
                            "suv" -> LocaleStrings.get("suv", language)
                            "electric" -> LocaleStrings.get("electric", language)
                            "sports" -> LocaleStrings.get("sports", language)
                            else -> ""
                        }
                        val vehicleDisplayName = buildString {
                            if (carBrand.isNotEmpty()) {
                                append(carBrand)
                                append(" ")
                            }
                            if (carModelName.isNotEmpty()) {
                                append(carModelName)
                                append(" ")
                            }
                            if (carTypeLabel.isNotEmpty()) {
                                append("($carTypeLabel)")
                            }
                        }.trim()

                        Text(
                            text = if (vehicleDisplayName.isNotEmpty()) vehicleDisplayName.uppercase() else "DASHBOARD",
                            color = CockpitAccentRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.6.sp
                        )
                        Text(
                            text = LocaleStrings.get("app_title", language),
                            color = CockpitTextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    
                    // Language and Onboarding trigger chip
                    IconButton(
                        onClick = {
                            val nextLang = when (language) {
                                AppLanguage.FR -> AppLanguage.EN
                                AppLanguage.EN -> AppLanguage.AR
                                AppLanguage.AR -> AppLanguage.FR
                            }
                            viewModel.setLanguage(nextLang)
                            val name = when (nextLang) {
                                AppLanguage.FR -> "Français"
                                AppLanguage.EN -> "English"
                                AppLanguage.AR -> "العربية"
                            }
                            Toast.makeText(context, "Langue: $name", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(CockpitCard)
                            .size(36.dp)
                            .testTag("lang_toggle_btn")
                    ) {
                        Text(
                            text = when (language) {
                                AppLanguage.FR -> "🇫🇷"
                                AppLanguage.EN -> "🇬🇧"
                                AppLanguage.AR -> "🇩🇿"
                            },
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // INNER CONTENT SCROLLER BY ACTIVE TAB selection
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (activeTab) {
                    0 -> {
                        // 1. DASHBOARD OVERVIEW TAB
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
                        ) {
                            // Diagnostics warning banner if any active DTC fault or system alert
                            if (troubleCodes.isNotEmpty()) {
                                item {
                                    AlertBanner(
                                        icon = Icons.Default.Warning,
                                        title = "${LocaleStrings.get("diagnostic_trouble_codes", language)} (${troubleCodes.size})",
                                        desc = troubleCodes.joinToString(", "),
                                        color = CockpitAccentRed
                                    )
                                }
                            }

                            // Alert for timing belt replacement prediction based on maintenance entries
                            item {
                                val distribLogs = maintenanceLogs.filter { it.category == "Vidange" || it.title.contains("Vidange", true) }
                                val maxKmMaint = if (distribLogs.isNotEmpty()) distribLogs.maxOf { it.odometer } else 0
                                if (maxKmMaint > maxOilChangeDist) {
                                    AlertBanner(
                                        icon = Icons.Default.NotificationsActive,
                                        title = LocaleStrings.get("maintenance_required", language),
                                        desc = if (language == AppLanguage.FR) "Votre dernière vidange enregistrée date de ${maxKmMaint} km. Dépassement de l'intervalle recommandé de $maxOilChangeDist km." else "Last oil change recorded was at ${maxKmMaint} km. Exceeds standard threshold intervals.",
                                        color = CockpitAccentAmber
                                    )
                                }
                            }

                            // Bluetooth trigger card
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = CockpitSurface),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, CockpitGridDividers),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .clickable { showConnectionDialog = true }
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when (btState) {
                                                        BtState.CONNECTED -> CockpitAccentGreen.copy(alpha = 0.15f)
                                                        BtState.CONNECTING -> CockpitAccentAmber.copy(alpha = 0.15f)
                                                        BtState.DISCONNECTED -> CockpitTextSecondary.copy(alpha = 0.1f)
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Bluetooth,
                                                contentDescription = "Bluetooth Status",
                                                tint = when (btState) {
                                                    BtState.CONNECTED -> CockpitAccentGreen
                                                    BtState.CONNECTING -> CockpitAccentAmber
                                                    BtState.DISCONNECTED -> CockpitTextSecondary
                                                }
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (btState == BtState.CONNECTED) LocaleStrings.get("connected", language) else if (btState == BtState.CONNECTING) LocaleStrings.get("connecting", language) else LocaleStrings.get("disconnected", language),
                                                fontWeight = FontWeight.Bold,
                                                color = CockpitTextPrimary,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = if (btState == BtState.CONNECTED) "${LocaleStrings.get("active_device", language)}: $connectedDevice" else LocaleStrings.get("tap_to_search", language),
                                                color = CockpitTextSecondary,
                                                fontSize = 12.sp
                                            )
                                        }

                                        if (btState == BtState.CONNECTED) {
                                            IconButton(onClick = { viewModel.disconnectObd() }) {
                                                Icon(
                                                    imageVector = Icons.Default.PowerSettingsNew,
                                                    contentDescription = "Disconnect",
                                                    tint = CockpitAccentRed
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Dynamic instruments physical speed dial & engine RPM indicator
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = CockpitSurface),
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, CockpitGridDividers),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = LocaleStrings.get("instruments", language),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CockpitTextSecondary,
                                            letterSpacing = 1.2.sp
                                        )

                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.size(170.dp)
                                        ) {
                                            Canvas(modifier = Modifier.size(170.dp)) {
                                                drawArc(
                                                    brush = Brush.sweepGradient(
                                                        0.0f to CockpitAccentGreen,
                                                        0.5f to CockpitAccentAmber,
                                                        0.8f to CockpitAccentRed,
                                                        1.0f to CockpitAccentRed
                                                    ),
                                                    startAngle = 135f,
                                                    sweepAngle = 270f,
                                                    useCenter = false,
                                                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                                                )

                                                val targetAngle = 135f + (speedKmh.coerceIn(0f, 180f) / 180f * 270f)
                                                val rad = Math.toRadians(targetAngle.toDouble())
                                                val needleLen = size.width / 2 - 14.dp.toPx()
                                                val targetOffset = Offset(
                                                    center.x + needleLen * cos(rad).toFloat(),
                                                    center.y + needleLen * sin(rad).toFloat()
                                                )

                                                drawLine(
                                                    color = CockpitAccentRed,
                                                    start = center,
                                                    end = targetOffset,
                                                    strokeWidth = 3.dp.toPx(),
                                                    cap = StrokeCap.Round
                                                )

                                                drawCircle(color = CockpitAccentRed, radius = 5.dp.toPx())
                                            }

                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.offset(y = 20.dp)
                                            ) {
                                                Text(
                                                    text = String.format("%.0f", speedKmh),
                                                    fontSize = 42.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = CockpitTextPrimary,
                                                    lineHeight = 1.sp
                                                )
                                                Text(
                                                    text = "km/h",
                                                    fontSize = 12.sp,
                                                    color = CockpitTextSecondary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        // RPM Progress strip bar
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("RPM (x1000)", fontSize = 10.sp, color = CockpitTextSecondary)
                                                Text("${String.format("%.0f", rpm)} tr/min", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CockpitAccentGreen)
                                            }
                                            val progress = (rpm / 7000f).coerceIn(0f, 1f)
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(6.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(CockpitGridDividers)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxHeight()
                                                        .fillMaxWidth(progress)
                                                        .background(
                                                            Brush.horizontalGradient(
                                                                listOf(CockpitAccentGreen, CockpitAccentAmber, CockpitAccentRed)
                                                            )
                                                        )
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Telemetry Grid sensors
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        SensorPanel(
                                            modifier = Modifier.weight(1f),
                                            title = LocaleStrings.get("coolant", language),
                                            value = "${String.format("%.1f", engineTemp)} °C",
                                            indicatorColor = if (engineTemp > 98f) CockpitAccentRed else CockpitAccentBlue,
                                            icon = Icons.Default.Thermostat,
                                            description = LocaleStrings.get("coolant_desc", language)
                                        )

                                        SensorPanel(
                                            modifier = Modifier.weight(1f),
                                            title = LocaleStrings.get("battery", language),
                                            value = "${String.format("%.2f", batteryVolts)} V",
                                            indicatorColor = if (batteryVolts < minBatteryThresh) CockpitAccentRed else CockpitAccentGreen,
                                            icon = Icons.Default.ElectricCar,
                                            description = LocaleStrings.get("battery_desc", language)
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        SensorPanel(
                                            modifier = Modifier.weight(1f),
                                            title = LocaleStrings.get("fuel_rate", language),
                                            value = "${String.format("%.1f", fuelRate)} L/h",
                                            indicatorColor = CockpitAccentAmber,
                                            icon = Icons.Default.LocalGasStation,
                                            description = LocaleStrings.get("fuel_rate_desc", language)
                                        )

                                        SensorPanel(
                                            modifier = Modifier.weight(1f),
                                            title = LocaleStrings.get("throttle", language),
                                            value = "${String.format("%.0f", throttlePos)} %",
                                            indicatorColor = CockpitAccentBlue,
                                            icon = Icons.Default.Speed,
                                            description = LocaleStrings.get("throttle_desc", language)
                                        )
                                    }
                                }
                            }

                            // Tire pressure widget diagram
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = CockpitSurface),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, CockpitGridDividers),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = LocaleStrings.get("tires", language),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = CockpitTextSecondary,
                                            letterSpacing = 0.8.sp
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceAround
                                        ) {
                                            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                                                TireWidget(LocaleStrings.get("tire_front_left", language), tirePsiFL, minTireThresh)
                                                TireWidget(LocaleStrings.get("tire_rear_left", language), tirePsiRL, minTireThresh)
                                            }

                                            // Car drawing shape
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 60.dp, height = 110.dp)
                                                    .border(2.dp, CockpitAccentRed, RoundedCornerShape(8.dp))
                                                    .background(CockpitBackground, RoundedCornerShape(8.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DirectionsCar,
                                                    contentDescription = null,
                                                    tint = CockpitAccentRed,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }

                                            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                                                TireWidget(LocaleStrings.get("tire_front_right", language), tirePsiFR, minTireThresh)
                                                TireWidget(LocaleStrings.get("tire_rear_right", language), tirePsiRR, minTireThresh)
                                            }
                                        }

                                        Divider(color = CockpitGridDividers, modifier = Modifier.padding(top = 8.dp))
                                        Text(
                                            text = LocaleStrings.get("quick_test", language),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CockpitTextSecondary
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Button(
                                                onClick = { viewModel.setTirePressure(1.5f, 2.1f, 2.1f, 1.6f) },
                                                colors = ButtonDefaults.buttonColors(containerColor = CockpitAccentRed),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f).testTag("drop_tire_btn")
                                            ) {
                                                Text(LocaleStrings.get("drop_tires", language), fontSize = 11.sp)
                                            }
                                            Button(
                                                onClick = { viewModel.setTirePressure(2.1f, 2.1f, 2.1f, 2.1f) },
                                                colors = ButtonDefaults.buttonColors(containerColor = CockpitAccentGreen),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f).testTag("inflate_tire_btn")
                                            ) {
                                                Text(LocaleStrings.get("inflate_tires", language), fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // 2. ENTRETIEN / LOGBOOK LOGS TAB
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = LocaleStrings.get("total_spent", language),
                                        color = CockpitTextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${String.format("%.0f", totalMaintCost)} DA",
                                        color = CockpitAccentRed,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                                Button(
                                    onClick = { showAddLogBottomSheet = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = CockpitAccentRed),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("trigger_add_log_btn")
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(LocaleStrings.get("add_service", language), fontWeight = FontWeight.Bold)
                                }
                            }

                            // Notice tag
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CockpitSurface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                border = BorderStroke(1.dp, CockpitGridDividers)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = CockpitAccentAmber, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = LocaleStrings.get("fuel_cost_auto", language),
                                        color = CockpitTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            if (maintenanceLogs.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = LocaleStrings.get("no_services", language),
                                        textAlign = TextAlign.Center,
                                        color = CockpitTextSecondary
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(bottom = 90.dp)
                                ) {
                                    items(maintenanceLogs) { log ->
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = CockpitSurface),
                                            border = BorderStroke(1.dp, CockpitGridDividers),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(8.dp)
                                                                .clip(CircleShape)
                                                                .background(
                                                                    when (log.category) {
                                                                        "Vidange" -> CockpitAccentGreen
                                                                        "Repairs" -> CockpitAccentRed
                                                                        "Tires" -> CockpitAccentAmber
                                                                        else -> CockpitAccentBlue
                                                                    }
                                                                )
                                                        )
                                                        Text(
                                                            text = log.title,
                                                            fontWeight = FontWeight.Bold,
                                                            color = CockpitTextPrimary,
                                                            fontSize = 14.sp
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "Odomètre : ${log.odometer} km | ${log.date}",
                                                        fontSize = 12.sp,
                                                        color = CockpitTextSecondary
                                                    )
                                                    if (log.notes.isNotEmpty()) {
                                                        Text(
                                                            text = log.notes,
                                                            fontSize = 11.sp,
                                                            color = CockpitTextSecondary
                                                        )
                                                    }
                                                }
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(
                                                        text = "${String.format("%.0f", log.cost)} DA",
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 14.sp,
                                                        color = CockpitAccentRed
                                                    )
                                                    IconButton(onClick = { viewModel.deleteLog(log) }) {
                                                        Icon(Icons.Default.Delete, contentDescription = null, tint = CockpitTextSecondary, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        // 3. TRAJETS / TRIPS RECORDER TAB
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp)
                        ) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = CockpitSurface),
                                    shape = RoundedCornerShape(18.dp),
                                    border = BorderStroke(1.2.dp, if (isTrackingTrip) CockpitAccentAmber else CockpitGridDividers),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = LocaleStrings.get("active_trip", language).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = if (isTrackingTrip) CockpitAccentAmber else CockpitTextSecondary
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceAround
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(LocaleStrings.get("trip_dist", language), fontSize = 10.sp, color = CockpitTextSecondary)
                                                Text("${String.format("%.2f", activeTripDistance)} km", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CockpitTextPrimary)
                                            }
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(LocaleStrings.get("trip_dur", language), fontSize = 10.sp, color = CockpitTextSecondary)
                                                val durMin = activeTripDuration / 60
                                                val durSec = activeTripDuration % 60
                                                Text(String.format("%02d:%02d", durMin, durSec), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CockpitTextPrimary)
                                            }
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(LocaleStrings.get("trip_fuel", language), fontSize = 10.sp, color = CockpitTextSecondary)
                                                Text("${String.format("%.2f", activeTripFuel)} L", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CockpitAccentGreen)
                                            }
                                        }

                                        // Canvas Map
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(100.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(CockpitBackground)
                                                .border(1.dp, CockpitGridDividers, RoundedCornerShape(10.dp))
                                        ) {
                                            Canvas(modifier = Modifier.fillMaxSize()) {
                                                val stepWidth = size.width / 10f
                                                for (i in 0..10) {
                                                    drawLine(color = CockpitGridDividers.copy(alpha = 0.3f), start = Offset(i * stepWidth, 0f), end = Offset(i * stepWidth, size.height), strokeWidth = 1f)
                                                }
                                                val stepHeight = size.height / 5f
                                                for (i in 0..5) {
                                                    drawLine(color = CockpitGridDividers.copy(alpha = 0.3f), start = Offset(0f, i * stepHeight), end = Offset(size.width, i * stepHeight), strokeWidth = 1f)
                                                }

                                                if (activeGpsPoints.size >= 2) {
                                                    val minLng = activeGpsPoints.minOf { it.second }
                                                    val maxLng = activeGpsPoints.maxOf { it.second }
                                                    val minLat = activeGpsPoints.minOf { it.first }
                                                    val maxLat = activeGpsPoints.maxOf { it.first }

                                                    val lngRange = if (maxLng != minLng) maxLng - minLng else 1.0
                                                    val latRange = if (maxLat != minLat) maxLat - minLat else 1.0

                                                    for (idx in 0 until activeGpsPoints.size - 1) {
                                                        val pt1 = activeGpsPoints[idx]
                                                        val pt2 = activeGpsPoints[idx + 1]

                                                        val x1 = ((pt1.second - minLng) / lngRange * (size.width - 40.dp.toPx())).toFloat() + 20.dp.toPx()
                                                        val y1 = size.height - (((pt1.first - minLat) / latRange * (size.height - 40.dp.toPx())).toFloat() + 20.dp.toPx())
                                                        val x2 = ((pt2.second - minLng) / lngRange * (size.width - 40.dp.toPx())).toFloat() + 20.dp.toPx()
                                                        val y2 = size.height - (((pt2.first - minLat) / latRange * (size.height - 40.dp.toPx())).toFloat() + 20.dp.toPx())

                                                        drawLine(
                                                            color = CockpitAccentRed,
                                                            start = Offset(x1, y1),
                                                            end = Offset(x2, y2),
                                                            strokeWidth = 3.dp.toPx(),
                                                            cap = StrokeCap.Round
                                                        )
                                                    }

                                                    val lastPt = activeGpsPoints.last()
                                                    val lastX = ((lastPt.second - minLng) / lngRange * (size.width - 40.dp.toPx())).toFloat() + 20.dp.toPx()
                                                    val lastY = size.height - (((lastPt.first - minLat) / latRange * (size.height - 40.dp.toPx())).toFloat() + 20.dp.toPx())
                                                    drawCircle(color = CockpitAccentAmber, radius = 6.dp.toPx(), center = Offset(lastX, lastY))
                                                }
                                            }

                                            Text(
                                                text = LocaleStrings.get("gps_points_connected", language),
                                                fontSize = 8.sp,
                                                color = CockpitAccentAmber,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(6.dp).align(Alignment.BottomStart)
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            if (!isTrackingTrip) {
                                                Button(
                                                    onClick = { viewModel.startTrackTrip() },
                                                    colors = ButtonDefaults.buttonColors(containerColor = CockpitAccentGreen),
                                                    shape = RoundedCornerShape(10.dp),
                                                    modifier = Modifier.fillMaxWidth().testTag("start_trip_btn")
                                                ) {
                                                    Icon(Icons.Default.Map, contentDescription = null, tint = Color.White)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(LocaleStrings.get("start_trip_btn", language), fontWeight = FontWeight.Bold)
                                                }
                                            } else {
                                                Button(
                                                    onClick = { viewModel.stopTrackTrip() },
                                                    colors = ButtonDefaults.buttonColors(containerColor = CockpitAccentRed),
                                                    shape = RoundedCornerShape(10.dp),
                                                    modifier = Modifier.fillMaxWidth().testTag("stop_trip_btn")
                                                ) {
                                                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(LocaleStrings.get("stop_trip_btn", language), fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Drive Trip History List
                            item {
                                Text(
                                    text = LocaleStrings.get("trip_history", language).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = CockpitTextSecondary
                                )
                            }

                            if (driveTrips.isEmpty()) {
                                item {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = CockpitSurface),
                                        border = BorderStroke(1.dp, CockpitGridDividers),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = LocaleStrings.get("no_trips", language),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(20.dp).fillMaxWidth(),
                                            color = CockpitTextSecondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            } else {
                                items(driveTrips) { trip ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = CockpitSurface),
                                        border = BorderStroke(1.dp, CockpitGridDividers),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = CockpitAccentAmber, modifier = Modifier.size(16.dp))
                                                    Text(trip.date, fontWeight = FontWeight.Bold, color = CockpitTextPrimary, fontSize = 13.sp)
                                                }
                                                Text(
                                                    text = "${trip.distanceKm} km",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = CockpitAccentGreen,
                                                    fontSize = 14.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column {
                                                    Text(
                                                        text = "${LocaleStrings.get("trip_dur", language)}: ${trip.durationMinutes} min",
                                                        fontSize = 11.sp,
                                                        color = CockpitTextSecondary
                                                    )
                                                    Text(
                                                        text = "${LocaleStrings.get("trip_fuel", language)}: ${trip.fuelConsumedLiters} L",
                                                        fontSize = 11.sp,
                                                        color = CockpitTextSecondary
                                                    )
                                                }

                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(
                                                        text = "${LocaleStrings.get("avg_speed", language)}: ${trip.avgSpeed} km/h",
                                                        fontSize = 11.sp,
                                                        color = CockpitTextSecondary
                                                    )
                                                    Text(
                                                        text = "${LocaleStrings.get("max_speed", language)}: ${trip.maxSpeed} km/h",
                                                        fontSize = 11.sp,
                                                        color = CockpitTextSecondary
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))
                                            Divider(color = CockpitGridDividers)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "📍 ${trip.startLocation} -> ${trip.endLocation}",
                                                    fontSize = 10.sp,
                                                    color = CockpitAccentBlue,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                IconButton(
                                                    onClick = { viewModel.deleteTrip(trip) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = null, tint = CockpitAccentRed, modifier = Modifier.size(14.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    3 -> {
                        // 4. PERFORMANCE REPORTS & DIAGNOSTIC SCANNER TAB
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp)
                        ) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = CockpitSurface),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, CockpitGridDividers),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = LocaleStrings.get("perf_report", language).uppercase(),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp,
                                            color = CockpitAccentAmber,
                                            letterSpacing = 1.sp
                                        )

                                        val fuelEfficiency = if (totalDistanceSum > 0) (totalFuelSum / totalDistanceSum * 100.0) else 6.1
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(LocaleStrings.get("fuel_eff", language), fontSize = 12.sp, color = CockpitTextSecondary)
                                                Text("${String.format("%.2f", fuelEfficiency)} L/100km", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CockpitAccentGreen)
                                            }
                                            Icon(Icons.Default.Assessment, contentDescription = null, tint = CockpitAccentGreen, modifier = Modifier.size(36.dp))
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(LocaleStrings.get("repair_costs", language), fontSize = 12.sp, color = CockpitTextSecondary)
                                                Text("${String.format("%.0f", totalMaintCost)} DA", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CockpitAccentRed)
                                            }
                                            Icon(Icons.Default.Build, contentDescription = null, tint = CockpitAccentRed, modifier = Modifier.size(32.dp))
                                        }

                                        Divider(color = CockpitGridDividers)
                                        Button(
                                            onClick = {
                                                Toast.makeText(context, LocaleStrings.get("export_success", language), Toast.LENGTH_LONG).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = CockpitCard),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(LocaleStrings.get("export_report", language), color = CockpitTextPrimary)
                                        }
                                    }
                                }
                            }

                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = CockpitSurface),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, CockpitGridDividers),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = LocaleStrings.get("diagnostic_trouble_codes", language).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = CockpitTextSecondary
                                        )

                                        if (isScanningDTC) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                CircularProgressIndicator(color = CockpitAccentAmber, modifier = Modifier.size(24.dp))
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(LocaleStrings.get("scanning", language), fontSize = 11.sp, color = CockpitTextSecondary)
                                            }
                                        } else {
                                            if (troubleCodes.isEmpty()) {
                                                Text(LocaleStrings.get("no_dtc", language), fontSize = 12.sp, color = CockpitTextSecondary)
                                            } else {
                                                troubleCodes.forEach { code ->
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                        modifier = Modifier.padding(vertical = 2.dp)
                                                    ) {
                                                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(CockpitAccentRed))
                                                        Text(code, fontSize = 12.sp, color = CockpitAccentRed, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = { viewModel.startEcuScan() },
                                                colors = ButtonDefaults.buttonColors(containerColor = CockpitAccentAmber),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f).testTag("trigger_dtc_scan")
                                            ) {
                                                Text(LocaleStrings.get("scan_ecu", language), fontSize = 11.sp, color = CockpitBackground, fontWeight = FontWeight.Bold)
                                            }
                                            if (troubleCodes.isNotEmpty()) {
                                                Button(
                                                    onClick = { viewModel.clearTroubleCodes() },
                                                    colors = ButtonDefaults.buttonColors(containerColor = CockpitCard),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.weight(1f).testTag("clear_dtc_btn")
                                                ) {
                                                    Text(LocaleStrings.get("clear_codes", language), fontSize = 11.sp, color = CockpitTextPrimary)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    4 -> {
                        // 5. SETTINGS / CONFIGURATION TAB
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp)
                        ) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = CockpitSurface),
                                    border = BorderStroke(1.dp, CockpitGridDividers),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(
                                            text = LocaleStrings.get("car_chassis_profile", language).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = CockpitAccentAmber
                                        )

                                        val currentBrandName = buildString {
                                            if (!carManufacturer.isNullOrEmpty()) {
                                                append(carManufacturer?.uppercase())
                                                append(" ")
                                            }
                                            if (!carModel.isNullOrEmpty()) {
                                                append(carModel?.uppercase())
                                                append(" ")
                                            }
                                            val typeName = when (carType) {
                                                "sedan" -> LocaleStrings.get("sedan", language)
                                                "hatchback" -> LocaleStrings.get("hatchback", language)
                                                "suv" -> LocaleStrings.get("suv", language)
                                                "electric" -> LocaleStrings.get("electric", language)
                                                "sports" -> LocaleStrings.get("sports", language)
                                                else -> "Not selected"
                                            }
                                            if (typeName.isNotEmpty()) {
                                                append("($typeName)")
                                            }
                                        }

                                        Text(
                                            text = currentBrandName,
                                            color = CockpitTextPrimary,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp
                                        )

                                        Divider(color = CockpitGridDividers)

                                        OutlinedTextField(
                                            value = carManufacturer ?: "",
                                            onValueChange = { viewModel.setCarDetails(carType ?: "sedan", it, carModel ?: "") },
                                            label = { Text(LocaleStrings.get("car_manufacturer", language)) },
                                            placeholder = { Text(LocaleStrings.get("car_manufacturer_placeholder", language)) },
                                            modifier = Modifier.fillMaxWidth().testTag("settings_manufacturer_input"),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = CockpitAccentRed,
                                                unfocusedBorderColor = CockpitGridDividers,
                                                focusedLabelColor = CockpitAccentRed
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            singleLine = true
                                        )

                                        OutlinedTextField(
                                            value = carModel ?: "",
                                            onValueChange = { viewModel.setCarDetails(carType ?: "sedan", carManufacturer ?: "", it) },
                                            label = { Text(LocaleStrings.get("car_model", language)) },
                                            placeholder = { Text(LocaleStrings.get("car_model_placeholder", language)) },
                                            modifier = Modifier.fillMaxWidth().testTag("settings_model_input"),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = CockpitAccentRed,
                                                unfocusedBorderColor = CockpitGridDividers,
                                                focusedLabelColor = CockpitAccentRed
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            singleLine = true
                                        )

                                        Divider(color = CockpitGridDividers)
                                        Text(
                                            text = LocaleStrings.get("select_car_subtitle", language),
                                            fontSize = 11.sp,
                                            color = CockpitTextSecondary
                                        )

                                        val types = listOf("sedan", "hatchback", "suv", "electric", "sports")
                                        types.forEach { t ->
                                            Button(
                                                onClick = { viewModel.setCarDetails(t, carManufacturer ?: "", carModel ?: "") },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (carType == t) CockpitAccentRed else CockpitCard
                                                ),
                                                modifier = Modifier.fillMaxWidth().testTag("car_type_$t"),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(vertical = 4.dp)
                                            ) {
                                                Text(LocaleStrings.get(t, language), color = CockpitTextPrimary, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = CockpitSurface),
                                    border = BorderStroke(1.dp, CockpitGridDividers),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = LocaleStrings.get("language_selection", language).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = CockpitTextSecondary
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = { viewModel.setLanguage(AppLanguage.FR) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (language == AppLanguage.FR) CockpitAccentRed else CockpitCard
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f).testTag("set_fr_btn"),
                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                            ) {
                                                Text("Français 🇫🇷", fontSize = 11.sp, maxLines = 1)
                                            }
                                            Button(
                                                onClick = { viewModel.setLanguage(AppLanguage.EN) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (language == AppLanguage.EN) CockpitAccentRed else CockpitCard
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f).testTag("set_en_btn"),
                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                            ) {
                                                Text("English 🇬🇧", fontSize = 11.sp, maxLines = 1)
                                            }
                                            Button(
                                                onClick = { viewModel.setLanguage(AppLanguage.AR) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (language == AppLanguage.AR) CockpitAccentRed else CockpitCard
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f).testTag("set_ar_btn"),
                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                            ) {
                                                Text("العربية 🇩🇿", fontSize = 11.sp, maxLines = 1)
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = CockpitSurface),
                                    border = BorderStroke(1.dp, CockpitGridDividers),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = LocaleStrings.get("theme_selection", language).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = CockpitTextSecondary
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Button(
                                                onClick = { viewModel.setDarkMode(false) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (!isDarkMode) CockpitAccentRed else CockpitCard
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f).testTag("theme_light_btn")
                                            ) {
                                                Text(LocaleStrings.get("light_mode", language), fontSize = 12.sp)
                                            }
                                            Button(
                                                onClick = { viewModel.setDarkMode(true) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isDarkMode) CockpitAccentRed else CockpitCard
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f).testTag("theme_dark_btn")
                                            ) {
                                                Text(LocaleStrings.get("dark_mode", language), fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = CockpitSurface),
                                    border = BorderStroke(1.dp, CockpitGridDividers),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = LocaleStrings.get("safety_thresholds", language).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = CockpitTextSecondary
                                        )

                                        Column {
                                            Text("${LocaleStrings.get("min_tire_p", language)}: ${String.format("%.1f", minTireThresh)} Bar", fontSize = 11.sp, color = CockpitTextPrimary)
                                            Slider(
                                                value = minTireThresh,
                                                onValueChange = { viewModel.minTirePressureThreshold.value = it },
                                                valueRange = 1.0f..3.0f,
                                                colors = SliderDefaults.colors(thumbColor = CockpitAccentRed, activeTrackColor = CockpitAccentRed)
                                            )
                                        }

                                        Column {
                                            Text("${LocaleStrings.get("min_batt_v", language)}: ${String.format("%.1f", minBatteryThresh)} V", fontSize = 11.sp, color = CockpitTextPrimary)
                                            Slider(
                                                value = minBatteryThresh,
                                                onValueChange = { viewModel.minBatteryVoltsThreshold.value = it },
                                                valueRange = 10.0f..13.0f,
                                                colors = SliderDefaults.colors(thumbColor = CockpitAccentAmber, activeTrackColor = CockpitAccentAmber)
                                            )
                                        }

                                        Column {
                                            Text("${LocaleStrings.get("max_oil", language)}: ${maxOilChangeDist} km", fontSize = 11.sp, color = CockpitTextPrimary)
                                            Slider(
                                                value = maxOilChangeDist.toFloat(),
                                                onValueChange = { viewModel.maxOdometerBetweenOilChanges.value = it.toInt() },
                                                valueRange = 3000f..15000f,
                                                colors = SliderDefaults.colors(thumbColor = CockpitAccentGreen, activeTrackColor = CockpitAccentGreen)
                                            )
                                        }
                                    }
                                }
                            }

                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = CockpitSurface),
                                    border = BorderStroke(1.dp, CockpitGridDividers),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = LocaleStrings.get("cloud_sync", language).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = CockpitTextSecondary
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (cloudSyncState == SyncState.SYNCED) "Status: Safe (Synced Offline)" else "Status: Synchronizing...",
                                                color = if (cloudSyncState == SyncState.SYNCED) CockpitAccentGreen else CockpitAccentAmber,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (cloudSyncState == SyncState.SYNCING) {
                                                CircularProgressIndicator(color = CockpitAccentAmber, modifier = Modifier.size(16.dp))
                                            }
                                        }

                                        Text(
                                            text = "${LocaleStrings.get("last_synced", language)}: $lastSyncedTime",
                                            fontSize = 11.sp,
                                            color = CockpitTextSecondary
                                        )

                                        Button(
                                            onClick = { viewModel.triggerCloudSync(silent = false) },
                                            colors = ButtonDefaults.buttonColors(containerColor = CockpitAccentRed),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().testTag("sync_action_btn")
                                        ) {
                                            Text(LocaleStrings.get("sync_btn", language))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // BOTTOM NAVIGATION VIEW TABS BAR
            NavigationBar(
                containerColor = CockpitSurface,
                contentColor = CockpitTextSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CockpitAccentRed,
                        selectedTextColor = CockpitAccentRed,
                        unselectedIconColor = CockpitTextSecondary,
                        unselectedTextColor = CockpitTextSecondary,
                        indicatorColor = CockpitCard
                    ),
                    icon = { Icon(Icons.Default.DirectionsCar, null) },
                    label = { Text(LocaleStrings.get("dashboard", language), fontSize = 10.sp) }
                )

                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CockpitAccentRed,
                        selectedTextColor = CockpitAccentRed,
                        unselectedIconColor = CockpitTextSecondary,
                        unselectedTextColor = CockpitTextSecondary,
                        indicatorColor = CockpitCard
                    ),
                    icon = { Icon(Icons.Default.Build, null) },
                    label = { Text(LocaleStrings.get("maint_tab", language), fontSize = 10.sp) }
                )

                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CockpitAccentRed,
                        selectedTextColor = CockpitAccentRed,
                        unselectedIconColor = CockpitTextSecondary,
                        unselectedTextColor = CockpitTextSecondary,
                        indicatorColor = CockpitCard
                    ),
                    icon = { Icon(Icons.Default.Map, null) },
                    label = { Text(LocaleStrings.get("trips_tab", language), fontSize = 10.sp) }
                )

                NavigationBarItem(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CockpitAccentRed,
                        selectedTextColor = CockpitAccentRed,
                        unselectedIconColor = CockpitTextSecondary,
                        unselectedTextColor = CockpitTextSecondary,
                        indicatorColor = CockpitCard
                    ),
                    icon = { Icon(Icons.Default.Assessment, null) },
                    label = { Text(LocaleStrings.get("reports_tab", language), fontSize = 10.sp) }
                )

                NavigationBarItem(
                    selected = activeTab == 4,
                    onClick = { activeTab = 4 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CockpitAccentRed,
                        selectedTextColor = CockpitAccentRed,
                        unselectedIconColor = CockpitTextSecondary,
                        unselectedTextColor = CockpitTextSecondary,
                        indicatorColor = CockpitCard
                    ),
                    icon = { Icon(Icons.Default.Settings, null) },
                    label = { Text(LocaleStrings.get("settings_tab", language), fontSize = 10.sp) }
                )
            }
        }

        // Onboarding Selection for Car profile (at beginning)
        if (carType == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CockpitBackground.copy(alpha = 0.98f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CockpitSurface),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.2.dp, CockpitAccentRed),
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🚗 COCKPIT INITIALIZATION",
                            fontWeight = FontWeight.Black,
                            color = CockpitAccentRed,
                            fontSize = 11.sp,
                            letterSpacing = 1.6.sp
                        )

                        Text(
                            text = LocaleStrings.get("select_car_title", language),
                            fontWeight = FontWeight.ExtraBold,
                            color = CockpitTextPrimary,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = LocaleStrings.get("select_car_subtitle", language),
                            fontSize = 11.sp,
                            color = CockpitTextSecondary,
                            textAlign = TextAlign.Center
                        )

                        // 1. CAR TYPE SECTOR FLOW (Stylish compact grids)
                        Text(
                            text = LocaleStrings.get("car_chassis_profile", language).uppercase(),
                            color = CockpitAccentAmber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Start)
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val carProfilesList = listOf(
                                "sedan" to "Berline (Sedan)",
                                "hatchback" to "Citadine (Hatchback)",
                                "suv" to "SUV / Break / 4x4",
                                "electric" to "Electric (EV)",
                                "sports" to "Sport Coupe"
                            )
                            carProfilesList.forEach { (key, label) ->
                                Button(
                                    onClick = { tempCarType = key },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (tempCarType == key) CockpitAccentRed else CockpitCard
                                    ),
                                    border = BorderStroke(1.dp, if (tempCarType == key) CockpitAccentRed else CockpitGridDividers),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                        .testTag("onboard_car_$key")
                                ) {
                                    Text(
                                        text = label,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = CockpitTextPrimary
                                    )
                                }
                            }
                        }

                        Divider(color = CockpitGridDividers)

                        // 2. MAKER (MARK) INPUT WITH QUICK CLICKABLE BRAND CHIPS
                        Text(
                            text = LocaleStrings.get("car_manufacturer", language).uppercase(),
                            color = CockpitAccentAmber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Start)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val brandsSuggestion = listOf("Toyota", "Lifan", "Renault", "Hyundai")
                            brandsSuggestion.forEach { brand ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(CockpitCard)
                                        .border(1.dp, if (tempManufacturer.equals(brand, ignoreCase = true)) CockpitAccentRed else CockpitGridDividers, RoundedCornerShape(8.dp))
                                        .clickable { tempManufacturer = brand }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(brand, color = CockpitTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = tempManufacturer,
                            onValueChange = { tempManufacturer = it },
                            label = { Text(LocaleStrings.get("car_manufacturer", language)) },
                            placeholder = { Text(LocaleStrings.get("car_manufacturer_placeholder", language)) },
                            modifier = Modifier.fillMaxWidth().testTag("onboard_manufacturer_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CockpitAccentRed,
                                unfocusedBorderColor = CockpitGridDividers,
                                focusedLabelColor = CockpitAccentRed
                            ),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )

                        // 3. MODEL INPUT
                        OutlinedTextField(
                            value = tempModel,
                            onValueChange = { tempModel = it },
                            label = { Text(LocaleStrings.get("car_model", language)) },
                            placeholder = { Text(LocaleStrings.get("car_model_placeholder", language)) },
                            modifier = Modifier.fillMaxWidth().testTag("onboard_model_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CockpitAccentRed,
                                unfocusedBorderColor = CockpitGridDividers,
                                focusedLabelColor = CockpitAccentRed
                            ),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // 4. ACTION SUBMISSION BUTTON
                        Button(
                            onClick = {
                                viewModel.setCarDetails(
                                    tempCarType,
                                    tempManufacturer.trim(),
                                    tempModel.trim()
                                )
                            },
                            enabled = tempManufacturer.isNotBlank() && tempModel.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CockpitAccentRed,
                                disabledContainerColor = CockpitCard
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("onboard_confirm_btn")
                        ) {
                            Text(
                                text = "CONFIRM PROFILE & START",
                                fontWeight = FontWeight.ExtraBold,
                                color = if (tempManufacturer.isNotBlank() && tempModel.isNotBlank()) Color.White else CockpitTextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // Add Maintenance Log Bottom Sheet Modal
        if (showAddLogBottomSheet) {
            AlertDialog(
                onDismissRequest = { showAddLogBottomSheet = false },
                containerColor = CockpitSurface,
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = CockpitAccentRed),
                        onClick = { showAddLogBottomSheet = false }
                    ) {
                        Text("Close", color = Color.White)
                    }
                },
                title = {
                    Text(
                        text = LocaleStrings.get("add_service", language),
                        fontWeight = FontWeight.Bold,
                        color = CockpitTextPrimary,
                        fontSize = 18.sp
                    )
                },
                text = {
                    var titleVal by remember { mutableStateOf("") }
                    var categoryVal by remember { mutableStateOf("Vidange") }
                    var costVal by remember { mutableStateOf("") }
                    var mileageVal by remember { mutableStateOf("") }
                    var notesVal by remember { mutableStateOf("") }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                    ) {
                        item {
                            OutlinedTextField(
                                value = titleVal,
                                onValueChange = { titleVal = it },
                                label = { Text(LocaleStrings.get("title_label", language)) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CockpitAccentRed,
                                    unfocusedBorderColor = CockpitGridDividers,
                                    focusedLabelColor = CockpitAccentRed
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_title")
                            )
                        }

                        item {
                            Text(LocaleStrings.get("category_label", language), color = CockpitTextSecondary, fontSize = 11.sp)
                            val cats = listOf("Vidange", "Repairs", "Tires", "Fuel", "Battery")
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                              ) {
                                cats.forEach { c ->
                                    FilterChip(
                                        selected = categoryVal == c,
                                        onClick = { categoryVal = c },
                                        label = { Text(c) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = CockpitAccentRed
                                        )
                                    )
                                }
                            }
                        }

                        item {
                            OutlinedTextField(
                                value = costVal,
                                onValueChange = { costVal = it },
                                label = { Text(LocaleStrings.get("cost_label", language)) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CockpitAccentRed,
                                    unfocusedBorderColor = CockpitGridDividers,
                                    focusedLabelColor = CockpitAccentRed
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_cost")
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = mileageVal,
                                onValueChange = { mileageVal = it },
                                label = { Text(LocaleStrings.get("mileage_label", language)) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CockpitAccentRed,
                                    unfocusedBorderColor = CockpitGridDividers,
                                    focusedLabelColor = CockpitAccentRed
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_mileage")
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = notesVal,
                                onValueChange = { notesVal = it },
                                label = { Text(LocaleStrings.get("notes_label", language)) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CockpitAccentRed,
                                    unfocusedBorderColor = CockpitGridDividers,
                                    focusedLabelColor = CockpitAccentRed
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_notes")
                            )
                        }

                        item {
                            Button(
                                onClick = {
                                    val costNum = costVal.toDoubleOrNull() ?: 0.0
                                    val milNum = mileageVal.toIntOrNull() ?: 0
                                    if (titleVal.isNotEmpty()) {
                                        viewModel.addMaintenanceCost(
                                            title = titleVal,
                                            category = categoryVal,
                                            cost = costNum,
                                            odometer = milNum,
                                            notes = notesVal
                                        )
                                        showAddLogBottomSheet = false
                                        Toast.makeText(context, "Log added offline!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Please enter title", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CockpitAccentRed),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("save_maint_btn")
                            ) {
                                Text(LocaleStrings.get("save_btn", language), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            )
        }

        // Bluetooth pair scanner selector
        if (showConnectionDialog) {
            AlertDialog(
                onDismissRequest = { showConnectionDialog = false },
                containerColor = CockpitSurface,
                confirmButton = {
                    TextButton(onClick = { showConnectionDialog = false }) {
                        Text("Close", color = CockpitAccentRed)
                    }
                },
                title = {
                    Text(
                        text = "Sync Bluetooth OBD Devices",
                        color = CockpitTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Paired bluetooth interfaces plugged into car ECU OBD connectors:",
                            fontSize = 12.sp,
                            color = CockpitTextSecondary
                        )
                        val devices = listOf(
                            "ELM327 OBD-II serial module",
                            "V-LINK Dongle Interface v2.2",
                            "Lifan OBD Cockpit coupler"
                        )
                        devices.forEach { device ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CockpitCard),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.connectToObd(device)
                                        showConnectionDialog = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Default.BluetoothConnected, contentDescription = null, tint = CockpitAccentGreen)
                                    Text(device, fontSize = 13.sp, color = CockpitTextPrimary)
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun AlertBanner(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String,
    color: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.40f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = title,
                    color = color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = desc,
                    color = CockpitTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun SensorPanel(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    indicatorColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CockpitSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CockpitGridDividers)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = CockpitTextSecondary,
                    letterSpacing = 0.5.sp
                )
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = indicatorColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = CockpitTextPrimary
            )
            Text(
                text = description,
                fontSize = 10.sp,
                color = CockpitTextSecondary,
                lineHeight = 12.sp
            )
        }
    }
}

@Composable
fun TireWidget(
    label: String,
    value: Float,
    threshold: Float
) {
    val isAlert = value < threshold
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.testTag("tire_${label.replace(" ", "_").lowercase()}")
    ) {
        val backgroundColor = if (isAlert) CockpitAccentRed.copy(alpha = 0.15f) else CockpitCard
        val borderStrokeColor = if (isAlert) CockpitAccentRed else CockpitGridDividers
        
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(backgroundColor)
                .border(1.5.dp, borderStrokeColor, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = String.format("%.1f", value),
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = if (isAlert) CockpitAccentRed else CockpitAccentGreen
            )
        }
        Text(
            text = label.uppercase(),
            fontSize = 9.sp,
            color = CockpitTextSecondary,
            fontWeight = FontWeight.Bold
        )
    }
}
