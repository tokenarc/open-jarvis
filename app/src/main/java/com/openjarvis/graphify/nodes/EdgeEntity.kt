package com.openjarvis.graphify.nodes

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "edge_entities",
    indices = [
        Index(value = ["fromId", "fromType"]),
        Index(value = ["toId", "toType"]),
        Index(value = ["edgeType"])
    ]
)
data class EdgeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fromId: Long,
    val fromType: String,
    val toId: Long,
    val toType: String,
    val edgeType: String,
    val weight: Float = 1f,
    val lastUpdated: Long = System.currentTimeMillis()
)

object EdgeTypes {
    const val TASK_TO_APP = "task_to_app"
    const val TASK_TO_PROVIDER = "task_to_provider"
    const val APP_TO_CONTACT = "app_to_contact"
    const val PATTERN_CONTAINS_TASK = "pattern_contains_task"
    const val TASK_TO_CONTACT = "task_to_contact"
    const val APP_LAUNCHED_AT = "app_launched_at"
}