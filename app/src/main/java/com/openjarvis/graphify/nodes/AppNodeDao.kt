package com.openjarvis.graphify.nodes

import androidx.room.*

@Dao
interface AppNodeDao {
    @Query("SELECT * FROM app_nodes ORDER BY lastUsed DESC LIMIT :limit")
    suspend fun getRecentApps(limit: Int): List<AppNode>
    
    @Query("SELECT * FROM app_nodes ORDER BY useCount DESC LIMIT :limit")
    suspend fun getMostUsedApps(limit: Int): List<AppNode>
    
    @Query("SELECT * FROM app_nodes WHERE packageName = :packageName LIMIT 1")
    suspend fun getByPackage(packageName: String): AppNode?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(appNode: AppNode): Long
    
    @Update
    suspend fun update(appNode: AppNode)
    
    @Query("UPDATE app_nodes SET lastUsed = :timestamp, useCount = useCount + 1 WHERE id = :id")
    suspend fun incrementUse(id: Long, timestamp: Long = System.currentTimeMillis())
}