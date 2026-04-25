package com.openjarvis.intelligence

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

@Entity(tableName = "app_intelligence")
data class AppIntelligenceEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val category: String,
    val capabilities: String,
    val trustScore: Float = 0.5f,
    val lastAnalyzed: Long = System.currentTimeMillis(),
    val isAIApp: Boolean = false,
    val aiMeta: String? = null
)

class Converters {
    @TypeConverter
    fun fromCapabilities(value: List<Capability>): String {
        return value.joinToString(",") { it.name }
    }
    
    @TypeConverter
    fun toCapabilities(value: String): List<Capability> {
        if (value.isBlank()) return emptyList()
        return value.split(",").map { Capability.valueOf(it.trim()) }
    }
}