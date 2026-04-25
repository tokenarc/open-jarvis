package com.openjarvis.automation

import android.content.Context
import androidx.room.*
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

@Entity(tableName = "automations")
data class AutomationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val command: String,
    val scheduleType: String,
    val scheduleHour: Int = 0,
    val scheduleMinute: Int = 0,
    val scheduleDayOfWeek: Int = 0,
    val scheduleIntervalMs: Long = 0,
    val enabled: Boolean = true,
    val lastRun: Long? = null,
    val lastResult: String? = null,
    val runCount: Int = 0
)

@Dao
interface AutomationDao {
    @Query("SELECT * FROM automations ORDER BY name")
    suspend fun getAll(): List<AutomationManager.Automation>
    
    @Query("SELECT * FROM automations WHERE id = :id")
    suspend fun getById(id: String): AutomationManager.Automation?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(automation: AutomationManager.Automation)
    
    @Update
    suspend fun update(automation: AutomationManager.Automation)
    
    @Query("DELETE FROM automations WHERE id = :id")
    suspend fun delete(id: String)
}

@Database(entities = [AutomationEntity::class], version = 1)
abstract class AutomationDB : RoomDatabase() {
    abstract fun automationDao(): AutomationDao
    
    companion object {
        @Volatile private var INSTANCE: AutomationDB? = null
        
        fun getInstance(context: Context): AutomationDB {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AutomationDB::class.java,
                    "automations.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

class AutomationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        val id = inputData.getString("automation_id") ?: return Result.failure()
        val command = inputData.getString("automation_command") ?: return Result.failure()
        
        return try {
            val db = AutomationDB.getInstance(applicationContext)
            val dao = db.automationDao()
            
            val automation = dao.getById(id) ?: return Result.failure()
            
            kotlinx.coroutines.delay(2000)
            
            val updated = automation.copy(
                lastRun = System.currentTimeMillis(),
                lastResult = "success",
                runCount = automation.runCount + 1
            )
            dao.update(updated)
            
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}