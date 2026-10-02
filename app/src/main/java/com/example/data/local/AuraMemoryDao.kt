package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AuraMemory
import kotlinx.coroutines.flow.Flow

@Dao
interface AuraMemoryDao {

    @Query("SELECT * FROM aura_memories ORDER BY timestamp DESC")
    fun getAllMemories(): Flow<List<AuraMemory>>

    @Query("SELECT * FROM aura_memories WHERE isActive = 1 ORDER BY timestamp DESC")
    fun getActiveMemories(): Flow<List<AuraMemory>>

    @Query("SELECT * FROM aura_memories WHERE isActive = 1 ORDER BY timestamp DESC")
    suspend fun getActiveMemoriesSync(): List<AuraMemory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: AuraMemory): Long

    @Update
    suspend fun updateMemory(memory: AuraMemory)

    @Query("DELETE FROM aura_memories WHERE id = :id")
    suspend fun deleteMemoryById(id: Long)

    @Query("DELETE FROM aura_memories")
    suspend fun clearAllMemories()
}
