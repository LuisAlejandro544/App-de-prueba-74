package com.example.data

import kotlinx.coroutines.flow.Flow

class KeyConfigRepository(private val keyConfigDao: KeyConfigDao) {
    val allConfigs: Flow<List<KeyConfig>> = keyConfigDao.getAllConfigs()

    suspend fun getConfigById(id: Int): KeyConfig? {
        return keyConfigDao.getConfigById(id)
    }

    suspend fun insertConfig(config: KeyConfig): Long {
        return keyConfigDao.insertConfig(config)
    }

    suspend fun deleteConfig(config: KeyConfig) {
        keyConfigDao.deleteConfig(config)
    }
}
