package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.VehicleDatabase
import com.example.data.model.DriveTrip
import com.example.data.model.MaintenanceLog
import com.example.data.repository.VehicleRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class VehicleViewModel(application: Application) : AndroidViewModel(application) {

    private val db = VehicleDatabase.getDatabase(application)
    private val repository = VehicleRepository(db.vehicleDao())
    private val prefs = application.getSharedPreferences("vehicle_prefs", Context.MODE_PRIVATE)

    // --- Localization State ---
    private val _language = MutableStateFlow(
        try {
            AppLanguage.valueOf(prefs.getString("selected_lang", AppLanguage.FR.name) ?: AppLanguage.FR.name)
        } catch (e: Exception) {
            AppLanguage.FR
        }
    )
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    // --- Onboarding Car Selection State ---
    private val _carType = MutableStateFlow<String?>(prefs.getString("selected_car_type", null))
    val carType: StateFlow<String?> = _carType.asStateFlow()

    // --- Car Make and Model State ---
    private val _carManufacturer = MutableStateFlow<String?>(prefs.getString("selected_car_manufacturer", null))
    val carManufacturer: StateFlow<String?> = _carManufacturer.asStateFlow()

    private val _carModel = MutableStateFlow<String?>(prefs.getString("selected_car_model", null))
    val carModel: StateFlow<String?> = _carModel.asStateFlow()

    // --- Light/Dark Theme State ---
    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("is_dark_mode", true))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun setLanguage(lang: AppLanguage) {
        _language.value = lang
        prefs.edit().putString("selected_lang", lang.name).apply()
    }

    fun setDarkMode(dark: Boolean) {
        _isDarkMode.value = dark
        prefs.edit().putBoolean("is_dark_mode", dark).apply()
    }

    fun setCarDetails(type: String, manufacturer: String, model: String) {
        _carType.value = type
        _carManufacturer.value = manufacturer
        _carModel.value = model
        prefs.edit()
            .putString("selected_car_type", type)
            .putString("selected_car_manufacturer", manufacturer)
            .putString("selected_car_model", model)
            .apply()
        
        recalibrateThresholds(type)
    }

    fun setCarType(type: String) {
        _carType.value = type
        prefs.edit().putString("selected_car_type", type).apply()
        recalibrateThresholds(type)
    }

    private fun recalibrateThresholds(type: String) {
        // Calibrate safety standards based on selected car types
        when (type) {
            "sports" -> {
                minTirePressureThreshold.value = 2.4f
                minBatteryVoltsThreshold.value = 12.0f
            }
            "suv" -> {
                minTirePressureThreshold.value = 2.3f
                minBatteryVoltsThreshold.value = 11.9f
            }
            "electric" -> {
                minTirePressureThreshold.value = 2.5f
                minBatteryVoltsThreshold.value = 12.2f
            }
            "hatchback" -> {
                minTirePressureThreshold.value = 1.9f
                minBatteryVoltsThreshold.value = 11.8f
            }
            "sedan" -> {
                minTirePressureThreshold.value = 2.1f
                minBatteryVoltsThreshold.value = 11.8f
            }
        }
    }

    // --- State Observables mapped from Room ---
    val maintenanceLogs: StateFlow<List<MaintenanceLog>> = repository.allMaintenanceLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val driveTrips: StateFlow<List<DriveTrip>> = repository.allDriveTrips
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalMaintenanceCost: StateFlow<Double> = repository.totalMaintenanceCost
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalDistance: StateFlow<Double> = repository.totalDistance
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalFuelConsumed: StateFlow<Double> = repository.totalFuelConsumed
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // --- Bluetooth OBD coupling Simulation State ---
    private val _btState = MutableStateFlow(BtState.DISCONNECTED)
    val btState: StateFlow<BtState> = _btState.asStateFlow()

    private val _connectedDevice = MutableStateFlow<String?>(null)
    val connectedDevice: StateFlow<String?> = _connectedDevice.asStateFlow()

    // --- Live Sensor Telemetry ---
    private val _speedKmh = MutableStateFlow(0f)
    val speedKmh: StateFlow<Float> = _speedKmh.asStateFlow()

    private val _rpm = MutableStateFlow(0f)
    val rpm: StateFlow<Float> = _rpm.asStateFlow()

    private val _engineTemp = MutableStateFlow(25f) // Starting cold degrees C
    val engineTemp: StateFlow<Float> = _engineTemp.asStateFlow()

    private val _throttlePos = MutableStateFlow(0f)
    val throttlePos: StateFlow<Float> = _throttlePos.asStateFlow()

    private val _batteryVolts = MutableStateFlow(12.3f) // Normal resting battery
    val batteryVolts: StateFlow<Float> = _batteryVolts.asStateFlow()

    private val _fuelRate = MutableStateFlow(0.0f) // Liters per hour
    val fuelRate: StateFlow<Float> = _fuelRate.asStateFlow()

    // Tire Pressure in Bars (Simulated for each wheel)
    private val _tirePsiFL = MutableStateFlow(2.1f)
    val tirePsiFL: StateFlow<Float> = _tirePsiFL.asStateFlow()

    private val _tirePsiFR = MutableStateFlow(2.1f)
    val tirePsiFR: StateFlow<Float> = _tirePsiFR.asStateFlow()

    private val _tirePsiRL = MutableStateFlow(2.1f)
    val tirePsiRL: StateFlow<Float> = _tirePsiRL.asStateFlow()

    private val _tirePsiRR = MutableStateFlow(2.1f)
    val tirePsiRR: StateFlow<Float> = _tirePsiRR.asStateFlow()

    // DTC diagnostic trouble codes
    private val _troubleCodes = MutableStateFlow<List<String>>(emptyList())
    val troubleCodes: StateFlow<List<String>> = _troubleCodes.asStateFlow()

    private val _isScanningDTC = MutableStateFlow(false)
    val isScanningDTC: StateFlow<Boolean> = _isScanningDTC.asStateFlow()

    // --- Customizable Alerts Thresholds (Settings) ---
    val minTirePressureThreshold = MutableStateFlow(1.8f) // Alert below this bar
    val minBatteryVoltsThreshold = MutableStateFlow(11.8f) // Alert below this voltage
    val maxOdometerBetweenOilChanges = MutableStateFlow(5000) // Algerian dust standard

    // --- Active GPS Trip Tracking telemetry ---
    private val _isTrackingTrip = MutableStateFlow(false)
    val isTrackingTrip: StateFlow<Boolean> = _isTrackingTrip.asStateFlow()

    private val _activeTripDistance = MutableStateFlow(0.0)
    val activeTripDistance: StateFlow<Double> = _activeTripDistance.asStateFlow()

    private val _activeTripDuration = MutableStateFlow(0) // dynamic duration in seconds
    val activeTripDuration: StateFlow<Int> = _activeTripDuration.asStateFlow()

    private val _activeTripFuel = MutableStateFlow(0.0)
    val activeTripFuel: StateFlow<Double> = _activeTripFuel.asStateFlow()

    // Coordinates for the simulated track map
    private val _activeGpsPoints = MutableStateFlow<List<Pair<Double, Double>>>(emptyList())
    val activeGpsPoints: StateFlow<List<Pair<Double, Double>>> = _activeGpsPoints.asStateFlow()

    // --- Cloud Sync telemetry ---
    private val _cloudSyncState = MutableStateFlow(SyncState.SYNCED)
    val cloudSyncState: StateFlow<SyncState> = _cloudSyncState.asStateFlow()

    private val _lastSyncedTime = MutableStateFlow("Never")
    val lastSyncedTime: StateFlow<String> = _lastSyncedTime.asStateFlow()

    private var simulationJob: Job? = null
    private var tripTrackingJob: Job? = null

    init {
        // Automatically start background sync monitor
        _lastSyncedTime.value = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
    }

    // --- Trigger simulated Bluetooth OBD Pairing ---
    fun connectToObd(deviceName: String) {
        _btState.value = BtState.CONNECTING
        _connectedDevice.value = deviceName
        viewModelScope.launch {
            // Simulate standard handshakes over bluetooth serial port (ELM327)
            delay(1500)
            _btState.value = BtState.CONNECTED
            // Start reading sensors
            startSensorTelemetry()
        }
    }

    fun disconnectObd() {
        stopSensorTelemetry()
        _btState.value = BtState.DISCONNECTED
        _connectedDevice.value = null
        // Reset speedometer diagnostics
        _speedKmh.value = 0f
        _rpm.value = 0f
        _fuelRate.value = 0f
        _batteryVolts.value = 12.3f
        _throttlePos.value = 0f
    }

    private fun startSensorTelemetry() {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            var waveTime = 0.0
            _batteryVolts.value = 14.1f // Alternator starts charging
            while (true) {
                delay(1000)
                waveTime += 0.1
                
                // If tracking a trip, simulate standard driving fluctuations
                if (_isTrackingTrip.value) {
                    val baseSpeed = 45f + (Math.sin(waveTime) * 15f).toFloat() // 30-60 km/h
                    _speedKmh.value = if (baseSpeed < 0) 0f else baseSpeed
                    _rpm.value = 1800f + (Math.sin(waveTime * 2.5) * 400f).toFloat()
                    _throttlePos.value = 25f + (Math.cos(waveTime) * 10f).toFloat()
                    
                    // Fuel flow corresponds to speed and RPM (L/h)
                    _fuelRate.value = 4.2f + (_rpm.value / 1000f) * 1.5f + (_speedKmh.value / 100f) * 2f
                    
                    // Warm up engine safely
                    if (_engineTemp.value < 90f) {
                        _engineTemp.value += 1.2f
                    } else {
                        _engineTemp.value = 90f + (Math.sin(waveTime) * 1.5f).toFloat()
                    }
                } else {
                    // Engine idling
                    _speedKmh.value = 0f
                    _rpm.value = 850f + (Math.sin(waveTime) * 30f).toFloat()
                    _throttlePos.value = 11f
                    _fuelRate.value = 0.9f + (Math.cos(waveTime) * 0.1).toFloat()
                    
                    if (_engineTemp.value < 85f) {
                        _engineTemp.value += 0.5f
                    } else {
                        _engineTemp.value = 85f + (Math.sin(waveTime * 0.5) * 0.5).toFloat()
                    }
                }
                
                // Slight battery fluctuations
                _batteryVolts.value = 14.1f + (Math.sin(waveTime * 3) * 0.08).toFloat()
            }
        }
    }

    private fun stopSensorTelemetry() {
        simulationJob?.cancel()
        simulationJob = null
    }

    // --- Adjust simulated Tire Pressures to enable warnings easily ---
    fun setTirePressure(frontLeft: Float, frontRight: Float, rearLeft: Float, rearRight: Float) {
        _tirePsiFL.value = frontLeft
        _tireFrPR(frontRight)
        _tireRlPR(rearLeft)
        _tireRrPR(rearRight)
    }

    private fun _tireFrPR(v: Float) { _tirePsiFR.value = v }
    private fun _tireRlPR(v: Float) { _tirePsiRL.value = v }
    private fun _tireRrPR(v: Float) { _tirePsiRR.value = v }

    fun adjustBatteryVoltage(volts: Float) {
        _batteryVolts.value = volts
    }

    // --- GPS Trip Tracking Operations ---
    fun startTrackTrip() {
        if (_btState.value != BtState.CONNECTED) {
            // Auto connect OBD if not connected before starting a drive!
            connectToObd("OBD-II ELM327 Adapter")
        }
        _isTrackingTrip.value = true
        _activeTripDistance.value = 0.0
        _activeTripDuration.value = 0
        _activeTripFuel.value = 0.0
        _activeGpsPoints.value = listOf(Pair(36.2625, 2.7562)) // Central Algeria starting route
        
        tripTrackingJob?.cancel()
        tripTrackingJob = viewModelScope.launch {
            var elapsedSeconds = 0
            while (_isTrackingTrip.value) {
                delay(1000)
                elapsedSeconds++
                _activeTripDuration.value = elapsedSeconds
                
                // Telemetry simulation increments (approx meter accumulation)
                val currentSpeed = _speedKmh.value
                val metersInSecond = currentSpeed / 3.6
                val addedDistanceKm = (metersInSecond / 1000.0)
                _activeTripDistance.value += addedDistanceKm
                
                // Liters of fuel spent = Flow rate (L/hour) * consumed fraction of an hour
                val currentHourlyFuelRate = _fuelRate.value
                val fuelLitersInSecond = currentHourlyFuelRate / 3600.0
                _activeTripFuel.value += fuelLitersInSecond

                // Accumulate dynamic GPS lines walking north-east
                val prevPoint = _activeGpsPoints.value.lastOrNull() ?: Pair(36.2625, 2.7562)
                val latShift = 0.00010 + (Math.sin(elapsedSeconds.toDouble() / 10.0) * 0.00004)
                val lonShift = 0.00012 + (Math.cos(elapsedSeconds.toDouble() / 10.0) * 0.00005)
                val listCopy = _activeGpsPoints.value.toMutableList()
                listCopy.add(Pair(prevPoint.first + latShift, prevPoint.second + lonShift))
                _activeGpsPoints.value = listCopy
            }
        }
    }

    fun stopTrackTrip(startLoc: String = "Tegart Street", endLoc: String = "National Highway") {
        _isTrackingTrip.value = false
        tripTrackingJob?.cancel()
        tripTrackingJob = null
        
        val distance = _activeTripDistance.value
        val fuelSpent = _activeTripFuel.value
        val durationSecs = _activeTripDuration.value
        val minutes = (durationSecs / 60) + 1
        
        if (distance > 0.02) { // ensure some telemetry logged
            viewModelScope.launch {
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val nowTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                val startTimeCal = Calendar.getInstance()
                startTimeCal.add(Calendar.SECOND, -durationSecs)
                val startTimeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(startTimeCal.time)
                
                // Avg Speed = km / hours
                val avgSpeed = if (durationSecs > 0) (distance / (durationSecs / 3600.0)) else 0.0
                val trip = DriveTrip(
                    date = today,
                    startTime = startTimeStr,
                    endTime = nowTime,
                    distanceKm = Math.round(distance * 100.0) / 100.0,
                    durationMinutes = minutes,
                    fuelConsumedLiters = Math.round(fuelSpent * 100.0) / 100.0,
                    avgSpeed = Math.round(avgSpeed * 10.0) / 10.0,
                    maxSpeed = if (avgSpeed > 0) Math.round(avgSpeed * 1.35 * 10.0) / 10.0 else 0.0,
                    startLocation = startLoc,
                    endLocation = endLoc,
                    isSynced = false
                )
                repository.insertDriveTrip(trip)
                
                // Post sync routine
                triggerCloudSync(silent = true)
            }
        }
    }

    fun deleteTrip(trip: DriveTrip) {
        viewModelScope.launch {
            repository.deleteDriveTrip(trip)
        }
    }

    // --- DTC Scan ECU Simulation Operations ---
    fun startEcuScan() {
        _isScanningDTC.value = true
        _troubleCodes.value = emptyList()
        viewModelScope.launch {
            delay(2500) // Simulate protocol handshake scanning
            _isScanningDTC.value = false
            
            // 20% chance of random sensor fault for user play testing!
            val rand = Math.random()
            val list = mutableListOf<String>()
            if (rand < 0.3) {
                list.add("P0301 - Cylinder 1 Misfire Detected")
            }
            if (rand in 0.3..0.5) {
                list.add("P0113 - Intake Air Temperature Sensor 1 Circuit High")
            }
            if (rand > 0.85) {
                list.add("P0420 - Catalyst System Efficiency Below Threshold")
            }
            if (_tirePsiFL.value < minTirePressureThreshold.value || _tirePsiFR.value < minTirePressureThreshold.value) {
                list.add("C0073 - Diagnostic Valve/Low Tire Pressure Alert Sensor Fault")
            }
            _troubleCodes.value = list
        }
    }

    fun clearTroubleCodes() {
        _isScanningDTC.value = true
        viewModelScope.launch {
            delay(1500)
            _isScanningDTC.value = false
            _troubleCodes.value = emptyList()
        }
    }

    // --- Maintenance Database Operations ---
    fun addMaintenanceCost(title: String, category: String, cost: Double, odometer: Int, notes: String = "") {
        viewModelScope.launch {
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val log = MaintenanceLog(
                title = title,
                category = category,
                cost = cost,
                odometer = odometer,
                date = dateStr,
                notes = notes,
                isSynced = false
            )
            repository.insertMaintenanceLog(log)
            
            triggerCloudSync(silent = true)
        }
    }

    fun deleteLog(log: MaintenanceLog) {
        viewModelScope.launch {
            repository.deleteMaintenanceLog(log)
        }
    }

    // --- Cloud Synchronization Simulation ---
    fun triggerCloudSync(silent: Boolean = false) {
        _cloudSyncState.value = SyncState.SYNCING
        viewModelScope.launch {
            delay(2000) // Simulated backend network roundtrip to Firebase or REST Server
            
            // Mark all items stored offline as Synced in Room
            repository.markAllLogsAsSynced()
            repository.markAllTripsAsSynced()
            
            _cloudSyncState.value = SyncState.SYNCED
            _lastSyncedTime.value = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopSensorTelemetry()
        tripTrackingJob?.cancel()
    }
}

enum class BtState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

enum class SyncState {
    SYNCED,
    SYNCING
}

enum class AppLanguage {
    EN, FR, AR
}
