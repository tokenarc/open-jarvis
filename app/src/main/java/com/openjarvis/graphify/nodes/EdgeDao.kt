package com.openjarvis.graphify.nodes

import androidx.room.*

@Dao
interface EdgeDao {
    @Query("SELECT * FROM edge_entities WHERE fromId = :fromId AND fromType = :fromType")
    suspend fun getOutgoingEdges(fromId: Long, fromType: String): List<EdgeEntity>
    
    @Query("SELECT * FROM edge_entities WHERE toId = :toId AND toType = :toType")
    suspend fun getIncomingEdges(toId: Long, toType: String): List<EdgeEntity>
    
    @Query("SELECT * FROM edge_entities WHERE fromId = :fromId AND fromType = :fromType AND edgeType = :type")
    suspend fun getEdgesByType(fromId: Long, fromType: String, type: String): List<EdgeEntity>
    
    @Query("""
        SELECT * FROM edge_entities 
        WHERE fromId IN (SELECT id FROM task_nodes WHERE timestamp > :since)
        AND (edgeType = :type OR :type = '')
        ORDER BY lastUpdated DESC
        LIMIT :limit
    """)
    suspend fun getRecentEdges(since: Long, type: String, limit: Int = 50): List<EdgeEntity>
    
    @Insert
    suspend fun insert(edge: EdgeEntity): Long
    
    @Delete
    suspend fun delete(edge: EdgeEntity)
    
    @Query("DELETE FROM edge_entities WHERE lastUpdated < :cutoff")
    suspend fun deleteOld(cutoff: Long)
}