package com.openjarvis.graphify.nodes

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contact_nodes")
data class ContactNode(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phoneNumber: String? = null,
    val email: String? = null,
    val lastContacted: Long = System.currentTimeMillis(),
    val contactCount: Int = 1,
    val contactMethod: String? = null
)