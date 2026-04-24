package com.openjarvis.graphify.nodes

import androidx.room.*

@Dao
interface ProviderNodeDao {
    @Query("SELECT * FROM provider_nodes ORDER BY useCount DESC LIMIT :limit")
    suspend fun getTopProviders(limit: Int): List<ProviderNode>
    
    @Query("SELECT * FROM provider_nodes WHERE providerName = :name LIMIT 1")
    suspend fun getByName(name: String): ProviderNode?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(providerNode: ProviderNode): Long
    
    @Update
    suspend fun update(providerNode: ProviderNode)
    
    @Query("UPDATE provider_nodes SET lastUsed = :timestamp, useCount = useCount + 1 WHERE id = :id")
    suspend fun incrementUse(id: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE provider_nodes SET successCount = successCount + 1, totalLatencyMs = totalLatencyMs + :latency WHERE id = :id")
    suspend fun recordSuccess(id: Long, latency: Long)
}