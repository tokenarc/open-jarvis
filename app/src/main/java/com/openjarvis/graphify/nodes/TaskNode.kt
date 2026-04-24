package com.openjarvis.graphify.nodes

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_nodes")
data class TaskNode(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val command: String,
    val result: String,
    val timestamp: Long = System.currentTimeMillis(),
    val providerUsed: String? = null,
    val latencyMs: Long = 0L
)