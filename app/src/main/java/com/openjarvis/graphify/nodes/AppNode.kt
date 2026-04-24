package com.openjarvis.graphify.nodes

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_nodes")
data class AppNode(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val label: String,
    val lastUsed: Long = System.currentTimeMillis(),
    val useCount: Int = 1
)