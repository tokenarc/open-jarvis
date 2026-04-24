package com.openjarvis.graphify

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.openjarvis.graphify.nodes.*

@Database(
    entities = [
        AppNode::class,
        TaskNode::class,
        ContactNode::class,
        PatternNode::class,
        ProviderNode::class,
        EdgeEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class GraphifyDB : RoomDatabase() {
    abstract fun appNodeDao(): AppNodeDao
    abstract fun taskNodeDao(): TaskNodeDao
    abstract fun contactNodeDao(): ContactNodeDao
    abstract fun patternNodeDao(): PatternNodeDao
    abstract fun providerNodeDao(): ProviderNodeDao
    abstract fun edgeDao(): EdgeDao
    
    companion object {
        @Volatile
        private var INSTANCE: GraphifyDB? = null
        
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS contact_nodes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        phoneNumber TEXT,
                        email TEXT,
                        lastContacted INTEGER NOT NULL,
                        contactCount INTEGER NOT NULL,
                        contactMethod TEXT
                    )
                """.trimIndent())
                
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS pattern_nodes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sequenceHash TEXT NOT NULL,
                        sequenceString TEXT NOT NULL,
                        occurrenceCount INTEGER NOT NULL,
                        lastSeen INTEGER NOT NULL,
                        confidence REAL NOT NULL,
                        timeOfDayMask INTEGER NOT NULL,
                        dayOfWeekMask INTEGER NOT NULL
                    )
                """.trimIndent())
                
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS provider_nodes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        providerName TEXT NOT NULL,
                        modelName TEXT,
                        lastUsed INTEGER NOT NULL,
                        useCount INTEGER NOT NULL,
                        successCount INTEGER NOT NULL,
                        totalLatencyMs INTEGER NOT NULL
                    )
                """.trimIndent())
                
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS edge_entities (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        fromId INTEGER NOT NULL,
                        fromType TEXT NOT NULL,
                        toId INTEGER NOT NULL,
                        toType TEXT NOT NULL,
                        edgeType TEXT NOT NULL,
                        weight REAL NOT NULL,
                        lastUpdated INTEGER NOT NULL
                    )
                """.trimIndent())
                
                db.execSQL("CREATE INDEX IF NOT EXISTS index_edge_from ON edge_entities(fromId, fromType)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_edge_to ON edge_entities(toId, toType)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_edge_type ON edge_entities(edgeType)")
            }
        }
        
        fun getInstance(context: Context): GraphifyDB {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GraphifyDB::class.java,
                    "graphify.db"
                )
                    .addMigrations(MIGRATION_1_2)
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