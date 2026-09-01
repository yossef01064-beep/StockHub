package com.local.fatateer.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleLogDao {
    @Query("SELECT * FROM sale_logs ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<SaleLog>>

    @Insert
    suspend fun insert(log: SaleLog)

    @Update
    suspend fun update(log: SaleLog)

    @Query("DELETE FROM sale_logs")
    suspend fun clearAll()

    @Delete
    suspend fun delete(log: SaleLog)
}
