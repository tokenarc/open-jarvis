package com.openjarvis.graphify

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.openjarvis.graphify.nodes.*
import com.openjarvis.intelligence.AppIntelligenceEntity
import com.openjarvis.intelligence.AppIntelligenceDao

@Database(
    entities = [
        AppNode::class,
        TaskNode::class,
        ContactNode::class,
        PatternNode::class,
        ProviderNode::class,
        EdgeEntity::class,
        AppIntelligenceEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class GraphifyDB : RoomDatabase() {
    abstract fun appNodeDao(): AppNodeDao
    abstract fun taskNodeDao(): TaskNodeDao
    abstract fun contactNodeDao(): ContactNodeDao
    abstract fun patternNodeDao(): PatternNodeDao
    abstract fun providerNodeDao(): ProviderNodeDao
    abstract fun edgeDao(): EdgeDao
    abstract fun appIntelligenceDao(): AppIntelligenceDao
    
    companion object {
        @Volatile
        private var INSTANCE: GraphifyDB? = null
        
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS app_intelligence (
                        packageName TEXT PRIMARY KEY NOT NULL,
                        appName TEXT NOT NULL,
                        category TEXT NOT NULL,
                        capabilities TEXT NOT NULL,
                        trustScore REAL NOT NULL DEFAULT 0.5,
                        lastAnalyzed INTEGER NOT NULL,
                        isAIApp INTEGER NOT NULL DEFAULT 0,
                        aiMeta TEXT
                    )
                """.trimIndent())
                
                db.execSQL("CREATE INDEX IF NOT EXISTS index_app_intel_category ON app_intelligence(category)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_app_intel_ai ON app_intelligence(isAIApp)")
            }
        }
        
        fun getInstance(context: Context): GraphifyDB {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GraphifyDB::class.java,
                    "graphify.db"
                )
                    .addMigrations(MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
        
        fun resetForTesting() {
            INSTANCE = null
        }
    }
}