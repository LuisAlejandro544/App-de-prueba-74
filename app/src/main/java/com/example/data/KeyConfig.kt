package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "key_configs")
data class KeyConfig(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val alias: String,
    val storePassword: String,
    val keyPassword: String,
    val validityYears: Int,
    val algorithm: String,
    val keySize: Int,
    val commonName: String,
    val organizationalUnit: String,
    val organization: String,
    val locality: String,
    val state: String,
    val countryCode: String,
    val timestamp: Long = System.currentTimeMillis(),
    val fileName: String,
    val filePath: String,
    val category: String = "PRODUCCIÓN"
)
