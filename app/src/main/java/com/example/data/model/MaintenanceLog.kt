package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "maintenance_logs")
data class MaintenanceLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String, // "Vidange", "Fuel", "Repairs", "Tires", "Battery", "Other"
    val cost: Double,     // cost of service
    val odometer: Int,    // mileage at service
    val date: String,     // YYYY-MM-DD
    val notes: String = "",
    val isSynced: Boolean = false
)
