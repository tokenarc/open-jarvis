package com.openjarvis.graphify.nodes

import androidx.room.*

@Dao
interface TaskNodeDao {
    @Query("SELECT * FROM task_nodes ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentTasks(limit: Int): List<TaskNode>
    
    @Query("SELECT * FROM task_nodes ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getTasksByCommand(command: String, limit: Int = 10): List<TaskNode>
    
    @Insert
    suspend fun insert(taskNode: TaskNode): Long
    
    @Query("DELETE FROM task_nodes WHERE timestamp < :cutoff")
    suspend fun deleteOld(cutoff: Long)
}