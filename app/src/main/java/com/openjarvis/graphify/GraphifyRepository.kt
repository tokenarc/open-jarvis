package com.openjarvis.graphify

import android.content.Context
import com.openjarvis.graphify.nodes.AppNode
import com.openjarvis.graphify.nodes.AppNodeDao
import com.openjarvis.graphify.nodes.TaskNode
import com.openjarvis.graphify.nodes.TaskNodeDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class GraphifyRepository(context: Context) {

    private val db = GraphifyDB.getInstance(context)
    private val appNodeDao: AppNodeDao = db.appNodeDao()
    private val taskNodeDao: TaskNodeDao = db.taskNodeDao()

    suspend fun logAppOpened(packageName: String, label: String) = withContext(Dispatchers.IO) {
        val existing = appNodeDao.getByPackage(packageName)
        if (existing != null) {
            appNodeDao.incrementUse(existing.id)
        } else {
            appNodeDao.insert(AppNode(packageName = packageName, label = label))
        }
    }

    suspend fun logTask(
        command: String,
        result: String,
        provider: String? = null,
        latencyMs: Long = 0L
    ) = withContext(Dispatchers.IO) {
        taskNodeDao.insert(
            TaskNode(
                command = command,
                result = result,
                providerUsed = provider,
                latencyMs = latencyMs
            )
        )
    }

    suspend fun getRecentApps(limit: Int = 10): List<AppNode> = withContext(Dispatchers.IO) {
        appNodeDao.getRecentApps(limit)
    }

    suspend fun getMostUsedApps(limit: Int = 10): List<AppNode> = withContext(Dispatchers.IO) {
        appNodeDao.getMostUsedApps(limit)
    }

    suspend fun getRecentTasks(limit: Int = 10): List<TaskNode> = withContext(Dispatchers.IO) {
        taskNodeDao.getRecentTasks(limit)
    }

    fun getRecentTasksFlow(limit: Int = 10): Flow<List<TaskNode>> = flow {
        emit(taskNodeDao.getRecentTasks(limit))
    }

    suspend fun searchTasks(query: String): List<TaskNode> = withContext(Dispatchers.IO) {
        taskNodeDao.getTasksByCommand(query, 10)
    }

    suspend fun getTaskStats(): TaskStats = withContext(Dispatchers.IO) {
        val all = taskNodeDao.getRecentTasks(1000)
        val total = all.size
        val today = all.filter { 
            it.timestamp > System.currentTimeMillis() - 24 * 60 * 60 * 1000 
        }.size
        val failed = all.count { 
            it.result.contains("Error") || it.result.contains("Failed") 
        }
        val failRate = if (total > 0) failed.toFloat() / total else 0f
        TaskStats(total, today, failRate)
    }

    data class TaskStats(
        val totalTasks: Int,
        val tasksToday: Int,
        val failRate: Float
    )
}