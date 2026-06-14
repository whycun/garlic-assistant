package com.whycun.garlicapp.data.local.dao

import androidx.room.*
import com.whycun.garlicapp.data.local.entity.Batch
import com.whycun.garlicapp.data.local.entity.CalcSave
import kotlinx.coroutines.flow.Flow

@Dao
interface BatchDao {
    @Query("SELECT * FROM batches ORDER BY createdAt DESC")
    fun getAll(): Flow<List<Batch>>

    @Query("SELECT * FROM batches WHERE status = 'holding' ORDER BY createdAt DESC")
    fun getHolding(): Flow<List<Batch>>

    @Query("SELECT * FROM batches WHERE id = :id")
    suspend fun getById(id: String): Batch?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(batch: Batch)

    @Update
    suspend fun update(batch: Batch)

    @Delete
    suspend fun delete(batch: Batch)

    @Query("DELETE FROM batches WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface CalcSaveDao {
    @Query("SELECT * FROM calc_saves ORDER BY createdAt DESC LIMIT 50")
    fun getAll(): Flow<List<CalcSave>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(calc: CalcSave)

    @Query("DELETE FROM calc_saves WHERE id = :id")
    suspend fun deleteById(id: String)
}
