package com.openjarvis.graphify

import android.content.Context
import com.openjarvis.graphify.nodes.AppNode
import com.openjarvis.graphify.nodes.AppNodeDao
import com.openjarvis.graphify.nodes.TaskNode
import com.openjarvis.graphify.nodes.TaskNodeDao
import kotlinx.coroutines.Dispatchers
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

    suspend fun logTask(command: String, result: String, providerUsed: String? = null) = withContext(Dispatchers.IO) {
        taskNodeDao.insert(TaskNode(command = command, result = result, providerUsed = providerUsed))
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
}