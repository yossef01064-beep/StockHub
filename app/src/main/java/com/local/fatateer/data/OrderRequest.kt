package com.local.fatateer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "order_requests")
data class OrderRequest(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemName: String,
    val itemImagePath: String? = null,
    val deviceName: String,
    val customerName: String,
    val customerPhone: String,
    val timestamp: Long = System.currentTimeMillis()
)