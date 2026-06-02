package com.example.data.repository

import com.example.data.dao.VehicleDao
import com.example.data.model.DriveTrip
import com.example.data.model.MaintenanceLog
import kotlinx.coroutines.flow.Flow

class VehicleRepository(private val vehicleDao: VehicleDao) {

    val allMaintenanceLogs: Flow<List<MaintenanceLog>> = vehicleDao.getAllMaintenanceLogs()
    val allDriveTrips: Flow<List<DriveTrip>> = vehicleDao.getAllDriveTrips()
    
    val totalMaintenanceCost: Flow<Double?> = vehicleDao.getTotalMaintenanceCostFlow()
    val totalDistance: Flow<Double?> = vehicleDao.getTotalDistanceFlow()
    val totalFuelConsumed: Flow<Double?> = vehicleDao.getTotalFuelConsumedFlow()

    suspend fun insertMaintenanceLog(log: MaintenanceLog): Long =
        vehicleDao.insertMaintenanceLog(log)

    suspend fun deleteMaintenanceLog(log: MaintenanceLog) =
        vehicleDao.deleteMaintenanceLog(log)

    suspend fun markAllLogsAsSynced() =
        vehicleDao.markAllLogsAsSynced()

    suspend fun insertDriveTrip(trip: DriveTrip): Long =
        vehicleDao.insertDriveTrip(trip)

    suspend fun deleteDriveTrip(trip: DriveTrip) =
        vehicleDao.deleteDriveTrip(trip)

    suspend fun markAllTripsAsSynced() =
        vehicleDao.markAllTripsAsSynced()
}
