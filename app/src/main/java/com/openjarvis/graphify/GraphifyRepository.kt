package com.openjarvis.graphify

import android.content.Context
import com.openjarvis.graphify.nodes.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.util.Calendar

class GraphifyRepository(context: Context) {

    private val db = GraphifyDB.getInstance(context)
    private val appNodeDao: AppNodeDao = db.appNodeDao()
    private val taskNodeDao: TaskNodeDao = db.taskNodeDao()
    private val contactNodeDao: ContactNodeDao = db.contactNodeDao()
    private val patternNodeDao: PatternNodeDao = db.patternNodeDao()
    private val providerNodeDao: ProviderNodeDao = db.providerNodeDao()
    private val edgeDao: EdgeDao = db.edgeDao()

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

    fun getTopAppsFlow(limit: Int = 5): Flow<List<AppNode>> = flow {
        emit(appNodeDao.getMostUsedApps(limit))
    }

    fun getTopContactFlow(): Flow<List<ContactNode>> = flow {
        emit(contactNodeDao.getRecentContacts(1))
    }

    fun getTopProviderFlow(): Flow<List<ProviderNode>> = flow {
        emit(providerNodeDao.getTopProviders(1))
    }

    fun getActivePatternCountFlow(): Flow<Int> = flow {
        val cutoff = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000
        emit(patternNodeDao.getActivePatterns(cutoff, 0.3f).size)
    }

    fun buildMemoryContext(command: String): String = kotlinx.coroutines.runBlocking {
        withContext(Dispatchers.IO) {
            val tasks = taskNodeDao.getRecentTasks(10)
            val patterns = patternNodeDao.getActivePatterns(
                System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000,
                0.3f
            )

            val sb = StringBuilder()
            sb.appendLine("## Recent Tasks")
            for (task in tasks.take(5)) {
                sb.appendLine("- ${task.command} → ${task.result}")
            }

            if (patterns.isNotEmpty()) {
                sb.appendLine("\n## Detected Patterns")
                for (pattern in patterns.take(3)) {
                    sb.appendLine("- ${pattern.sequenceString} (${(pattern.confidence * 100).toInt()}% confidence)")
                }
            }

            val contacts = contactNodeDao.getRecentContacts(3)
            if (contacts.isNotEmpty()) {
                sb.appendLine("\n## Frequent Contacts")
                for (contact in contacts) {
                    sb.appendLine("- ${contact.name}: ${contact.contactCount} interactions")
                }
            }

            sb.toString().take(1500)
        }
    }

    fun getSuggestions(): Flow<List<String>> = flow {
        val suggestions = mutableListOf<String>()
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val patterns = patternNodeDao.getActivePatterns(
            System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000,
            0.4f
        )

        for (pattern in patterns) {
            if (suggestions.size >= 3) break

            if (hour in 8..11 && (pattern.timeOfDayMask and (1 shl hour)) != 0) {
                val firstTask = pattern.sequenceString.split(" → ").firstOrNull() ?: continue
                if (firstTask.isNotBlank() && firstTask !in suggestions) {
                    suggestions.add(firstTask)
                }
            } else if (hour in 17..20 && (pattern.timeOfDayMask and (1 shl hour)) != 0) {
                val firstTask = pattern.sequenceString.split(" → ").firstOrNull() ?: continue
                if (firstTask.isNotBlank() && firstTask !in suggestions) {
                    suggestions.add(firstTask)
                }
            }
        }

        val contacts = contactNodeDao.getRecentContacts(3)
        for (contact in contacts) {
            if (suggestions.size >= 3) break
            val suggestion = "call ${contact.name}"
            if (suggestion !in suggestions) {
                suggestions.add(suggestion)
            }
        }

        emit(suggestions.take(3))
    }

    suspend fun insertContact(contact: ContactNode): Long = withContext(Dispatchers.IO) {
        contactNodeDao.insert(contact)
    }

    suspend fun findContactByName(name: String): ContactNode? = withContext(Dispatchers.IO) {
        contactNodeDao.findByNameOrPhone(name.lowercase(), "%${name}%")
    }

    suspend fun incrementContact(id: Long) = withContext(Dispatchers.IO) {
        contactNodeDao.incrementContact(id)
    }

    suspend fun insertPattern(pattern: PatternNode): Long = withContext(Dispatchers.IO) {
        patternNodeDao.insert(pattern)
    }

    suspend fun getPatternByHash(hash: String): PatternNode? = withContext(Dispatchers.IO) {
        patternNodeDao.getByHash(hash)
    }

    suspend fun getActivePatterns(since: Long, minConfidence: Float = 0.3f): List<PatternNode> =
        withContext(Dispatchers.IO) {
            patternNodeDao.getActivePatterns(since, minConfidence)
        }

    suspend fun getAllPatterns(): List<PatternNode> = withContext(Dispatchers.IO) {
        patternNodeDao.getTopPatterns(100)
    }

    suspend fun updatePatternConfidence(id: Long, confidence: Float, timestamp: Long = System.currentTimeMillis()) =
        withContext(Dispatchers.IO) {
            patternNodeDao.updateConfidence(id, confidence, timestamp)
        }

    suspend fun updatePatternTimeMask(id: Long, timeMask: Int) = withContext(Dispatchers.IO) {
        patternNodeDao.getByHash("").let { }
    }

    suspend fun insertProvider(provider: ProviderNode): Long = withContext(Dispatchers.IO) {
        providerNodeDao.insert(provider)
    }

    suspend fun getProviderByName(name: String): ProviderNode? = withContext(Dispatchers.IO) {
        providerNodeDao.getByName(name)
    }

    suspend fun incrementProviderUse(id: Long) = withContext(Dispatchers.IO) {
        providerNodeDao.incrementUse(id)
    }

    suspend fun recordProviderSuccess(id: Long, latency: Long) = withContext(Dispatchers.IO) {
        providerNodeDao.recordSuccess(id, latency)
    }

    suspend fun insertEdge(edge: EdgeEntity): Long = withContext(Dispatchers.IO) {
        edgeDao.insert(edge)
    }

    data class TaskStats(
        val totalTasks: Int,
        val tasksToday: Int,
        val failRate: Float
    )
    
    suspend fun getProviderStats(): List<ProviderStats> = withContext(Dispatchers.IO) {
        val providers = providerNodeDao.getTopProviders(10)
        providers.map { p ->
            ProviderStats(
                name = p.providerName,
                useCount = p.useCount,
                successRate = if (p.useCount > 0) p.successCount.toFloat() / p.useCount else 0f
            )
        }
    }
    
    data class ProviderStats(
        val name: String,
        val useCount: Int,
        val successRate: Float
    )
}