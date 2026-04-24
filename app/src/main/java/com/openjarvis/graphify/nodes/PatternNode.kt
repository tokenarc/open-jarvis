package com.openjarvis.graphify.nodes

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pattern_nodes")
data class PatternNode(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sequenceHash: String,
    val sequenceString: String,
    val occurrenceCount: Int = 1,
    val lastSeen: Long = System.currentTimeMillis(),
    val confidence: Float = 0.5f,
    val timeOfDayMask: Int = 0,
    val dayOfWeekMask: Int = 0
)