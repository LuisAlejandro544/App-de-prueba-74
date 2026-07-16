package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface KeyConfigDao {
    @Query("SELECT * FROM key_configs ORDER BY timestamp DESC")
    fun getAllConfigs(): Flow<List<KeyConfig>>

    @Query("SELECT * FROM key_configs WHERE id = :id LIMIT 1")
    suspend fun getConfigById(id: Int): KeyConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: KeyConfig): Long

    @Delete
    suspend fun deleteConfig(config: KeyConfig)
}
