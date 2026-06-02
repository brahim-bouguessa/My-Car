package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "drive_trips")
data class DriveTrip(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,             // YYYY-MM-DD
    val startTime: String,        // HH:MM
    val endTime: String,          // HH:MM
    val distanceKm: Double,       // Driven distance in km
    val durationMinutes: Int,     // Total duration of trip
    val fuelConsumedLiters: Double, // Fuel spent on the trip
    val avgSpeed: Double,         // Speed in km/h
    val maxSpeed: Double,         // Max speed in km/h
    val startLocation: String,     // Starting point
    val endLocation: String,       // Destination
    val isSynced: Boolean = false
)
