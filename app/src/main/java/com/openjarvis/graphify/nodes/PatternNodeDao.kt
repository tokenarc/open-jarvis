package com.openjarvis.graphify.nodes

import androidx.room.*

@Dao
interface PatternNodeDao {
    @Query("SELECT * FROM pattern_nodes ORDER BY confidence DESC LIMIT :limit")
    suspend fun getTopPatterns(limit: Int): List<PatternNode>
    
    @Query("SELECT * FROM pattern_nodes WHERE sequenceHash = :hash LIMIT 1")
    suspend fun getByHash(hash: String): PatternNode?
    
    @Query("SELECT * FROM pattern_nodes WHERE lastSeen > :since AND confidence > :minConfidence ORDER BY confidence DESC")
    suspend fun getActivePatterns(since: Long, minConfidence: Float = 0.3f): List<PatternNode>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(patternNode: PatternNode): Long
    
    @Update
    suspend fun update(patternNode: PatternNode)
    
    @Query("UPDATE pattern_nodes SET confidence = :confidence, lastSeen = :timestamp WHERE id = :id")
    suspend fun updateConfidence(id: Long, confidence: Float, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE pattern_nodes SET occurrenceCount = occurrenceCount + 1, lastSeen = :timestamp WHERE id = :id")
    suspend fun incrementOccurrence(id: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE pattern_nodes SET confidence = confidence * :decayFactor WHERE confidence > 0.1")
    suspend fun decayAll(decayFactor: Float)
    
    @Query("SELECT * FROM pattern_nodes WHERE id = :id")
    suspend fun getById(id: Long): PatternNode?
}