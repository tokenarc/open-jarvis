package com.openjarvis.graphify

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.openjarvis.graphify.nodes.AppNode
import com.openjarvis.graphify.nodes.AppNodeDao
import com.openjarvis.graphify.nodes.TaskNode
import com.openjarvis.graphify.nodes.TaskNodeDao

@Database(
    entities = [AppNode::class, TaskNode::class],
    version = 1,
    exportSchema = false
)
abstract class GraphifyDB : RoomDatabase() {
    abstract fun appNodeDao(): AppNodeDao
    abstract fun taskNodeDao(): TaskNodeDao
    
    companion object {
        @Volatile
        private var INSTANCE: GraphifyDB? = null
        
        fun getInstance(context: Context): GraphifyDB {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GraphifyDB::class.java,
                    "graphify.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}