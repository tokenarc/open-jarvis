package com.openjarvis.intelligence

import androidx.room.*

@Dao
interface AppIntelligenceDao {
    @Query("SELECT * FROM app_intelligence WHERE packageName = :packageName")
    suspend fun getByPackage(packageName: String): AppIntelligenceEntity?
    
    @Query("SELECT * FROM app_intelligence WHERE category = :category")
    suspend fun getByCategory(category: String): List<AppIntelligenceEntity>
    
    @Query("SELECT * FROM app_intelligence WHERE capabilities LIKE '%' || :capability || '%'")
    suspend fun getByCapability(capability: String): List<AppIntelligenceEntity>
    
    @Query("SELECT * FROM app_intelligence WHERE isAIApp = 1")
    suspend fun getAIApps(): List<AppIntelligenceEntity>
    
    @Query("SELECT * FROM app_intelligence ORDER BY trustScore DESC")
    suspend fun getAllSortedByTrust(): List<AppIntelligenceEntity>
    
    @Query("SELECT * FROM app_intelligence ORDER BY lastAnalyzed DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<AppIntelligenceEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: AppIntelligenceEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<AppIntelligenceEntity>)
    
    @Update
    suspend fun update(app: AppIntelligenceEntity)
    
    @Query("UPDATE app_intelligence SET trustScore = :score WHERE packageName = :packageName")
    suspend fun updateTrustScore(packageName: String, score: Float)
    
    @Query("SELECT COUNT(*) FROM app_intelligence")
    suspend fun getCount(): Int
    
    @Query("SELECT COUNT(*) FROM app_intelligence WHERE isAIApp = 1")
    suspend fun getAICount(): Int
}