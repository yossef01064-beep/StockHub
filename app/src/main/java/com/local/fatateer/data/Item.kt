package com.local.fatateer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    /** للريموتات: HD / SD / تلفزيون — باقي الأقسام فاضي */
    val subCategory: String = "",
    val brand: String = "",
    val quantity: Int,
    val minQuantity: Int = 1,
    val notes: String = "",
    /** مسار صورة المنتج داخل تخزين التطبيق. null = استخدم أيقونة التصنيف الافتراضية */
    val imagePath: String? = null,
    val priceMin: String = "",
    val priceMax: String = ""
)
