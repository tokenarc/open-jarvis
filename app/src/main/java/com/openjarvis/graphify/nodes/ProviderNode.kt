package com.openjarvis.graphify.nodes

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "provider_nodes")
data class ProviderNode(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val providerName: String,
    val modelName: String? = null,
    val lastUsed: Long = System.currentTimeMillis(),
    val useCount: Int = 1,
    val successCount: Int = 1,
    val totalLatencyMs: Long = 0L
)