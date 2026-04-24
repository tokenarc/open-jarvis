package com.openjarvis.graphify.nodes

import androidx.room.*

@Dao
interface ContactNodeDao {
    @Query("SELECT * FROM contact_nodes ORDER BY lastContacted DESC LIMIT :limit")
    suspend fun getRecentContacts(limit: Int): List<ContactNode>
    
    @Query("SELECT * FROM contact_nodes WHERE name LIKE :name OR phoneNumber LIKE :phone ORDER BY contactCount DESC LIMIT 1")
    suspend fun findByNameOrPhone(name: String, phone: String?): ContactNode?
    
    @Query("SELECT * FROM contact_nodes WHERE id = :id")
    suspend fun getById(id: Long): ContactNode?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contactNode: ContactNode): Long
    
    @Update
    suspend fun update(contactNode: ContactNode)
    
    @Query("UPDATE contact_nodes SET lastContacted = :timestamp, contactCount = contactCount + 1 WHERE id = :id")
    suspend fun incrementContact(id: Long, timestamp: Long = System.currentTimeMillis())
}