package com.local.fatateer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sale_logs")
data class SaleLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemName: String,
    val category: String,
    val price: String,
    val quantity: Int,
    val customerName: String = "",
    val customerPhone: String = "",
    val timestamp: Long = System.currentTimeMillis()
)