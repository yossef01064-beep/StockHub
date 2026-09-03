package com.local.fatateer.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderRequestDao {
    @Query("SELECT * FROM order_requests ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<OrderRequest>>

    @Insert
    suspend fun insert(request: OrderRequest)

    @Update
    suspend fun update(request: OrderRequest)

    @Delete
    suspend fun delete(request: OrderRequest)

    @Query("DELETE FROM order_requests")
    suspend fun clearAll()
}