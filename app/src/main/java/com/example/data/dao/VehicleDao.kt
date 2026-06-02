package com.example.data.dao

import androidx.room.*
import com.example.data.model.DriveTrip
import com.example.data.model.MaintenanceLog
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {

    // --- Maintenance Logs ---
    @Query("SELECT * FROM maintenance_logs ORDER BY odometer DESC, date DESC")
    fun getAllMaintenanceLogs(): Flow<List<MaintenanceLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaintenanceLog(log: MaintenanceLog): Long

    @Delete
    suspend fun deleteMaintenanceLog(log: MaintenanceLog)

    @Query("UPDATE maintenance_logs SET isSynced = 1 WHERE isSynced = 0")
    suspend fun markAllLogsAsSynced()

    // --- Drive Trips ---
    @Query("SELECT * FROM drive_trips ORDER BY date DESC, startTime DESC")
    fun getAllDriveTrips(): Flow<List<DriveTrip>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDriveTrip(trip: DriveTrip): Long

    @Delete
    suspend fun deleteDriveTrip(trip: DriveTrip)

    @Query("UPDATE drive_trips SET isSynced = 1 WHERE isSynced = 0")
    suspend fun markAllTripsAsSynced()

    // --- Stats Queries ---
    @Query("SELECT SUM(cost) FROM maintenance_logs")
    fun getTotalMaintenanceCostFlow(): Flow<Double?>

    @Query("SELECT SUM(distanceKm) FROM drive_trips")
    fun getTotalDistanceFlow(): Flow<Double?>

    @Query("SELECT SUM(fuelConsumedLiters) FROM drive_trips")
    fun getTotalFuelConsumedFlow(): Flow<Double?>
}
