
package com.openjarvis.automation

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Calendar
import java.util.concurrent.TimeUnit

class AutomationManager(private val context: Context) {

    private val db = AutomationDB.getInstance(context)
    private val dao = db.automationDao()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _automationsFlow =
        MutableStateFlow<List<Automation>>(emptyList())

    val automationsFlow: StateFlow<List<Automation>> = _automationsFlow

    suspend fun loadAutomations() = withContext(Dispatchers.IO) {
        _automationsFlow.value =
            dao.getAll().map { it.toAutomation() }
    }

    suspend fun createAutomation(
        automation: Automation
    ): String = withContext(Dispatchers.IO) {

        dao.insert(automation.toEntity())

        if (automation.enabled) {
            scheduleAutomation(automation)
        }

        _automationsFlow.value =
            dao.getAll().map { it.toAutomation() }

        automation.id
    }

    suspend fun updateAutomation(
        automation: Automation
    ) = withContext(Dispatchers.IO) {

        dao.update(automation.toEntity())

        cancelAutomation(automation.id)

        if (automation.enabled) {
            scheduleAutomation(automation)
        }

        _automationsFlow.value =
            dao.getAll().map { it.toAutomation() }
    }

    suspend fun deleteAutomation(id: String) =
        withContext(Dispatchers.IO) {

            cancelAutomation(id)
            dao.delete(id)

            _automationsFlow.value =
                dao.getAll().map { it.toAutomation() }
        }

    suspend fun toggleAutomation(
        id: String,
        enabled: Boolean
    ) = withContext(Dispatchers.IO) {

        val automation = dao.getById(id)?.toAutomation()
            ?: return@withContext

        val updated = automation.copy(enabled = enabled)

        dao.update(updated.toEntity())

        if (enabled) {
            scheduleAutomation(updated)
        } else {
            cancelAutomation(id)
        }

        _automationsFlow.value =
            dao.getAll().map { it.toAutomation() }
    }

    suspend fun runNow(id: String) = withContext(Dispatchers.IO) {

        val automation = dao.getById(id)?.toAutomation()
            ?: return@withContext

        if (!automation.enabled) return@withContext

        executeAutomation(automation)
    }

    private suspend fun scheduleAutomation(
        automation: Automation
    ) {
        if (!automation.enabled) return

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .build()

        val inputData = workDataOf(
            "automation_id" to automation.id,
            "automation_command" to automation.command
        )

        val request = when (val schedule = automation.schedule) {

            is AutomationSchedule.Daily -> {
                PeriodicWorkRequestBuilder<AutomationWorker>(
                    24, TimeUnit.HOURS,
                    15, TimeUnit.MINUTES
                )
                    .setConstraints(constraints)
                    .setInputData(inputData)
                    .setInitialDelay(
                        calculateDelay(schedule.hour, schedule.minute),
                        TimeUnit.MILLISECONDS
                    )
                    .addTag(automation.id)
                    .build()
            }

            is AutomationSchedule.Weekly -> {
                PeriodicWorkRequestBuilder<AutomationWorker>(
                    7, TimeUnit.DAYS,
                    15, TimeUnit.MINUTES
                )
                    .setConstraints(constraints)
                    .setInputData(inputData)
                    .setInitialDelay(
                        calculateWeeklyDelay(
                            schedule.dayOfWeek,
                            schedule.hour,
                            schedule.minute
                        ),
                        TimeUnit.MILLISECONDS
                    )
                    .addTag(automation.id)
                    .build()
            }

            is AutomationSchedule.Interval -> {
                val interval = schedule.intervalMs

                if (interval < 15 * 60 * 1000L) return

                PeriodicWorkRequestBuilder<AutomationWorker>(
                    interval,
                    TimeUnit.MILLISECONDS,
                    1,
                    TimeUnit.MINUTES
                )
                    .setConstraints(constraints)
                    .setInputData(inputData)
                    .addTag(automation.id)
                    .build()
            }

            is AutomationSchedule.Once -> {
                val delay =
                    schedule.atMs - System.currentTimeMillis()

                if (delay <= 0) return

                OneTimeWorkRequestBuilder<AutomationWorker>()
                    .setConstraints(constraints)
                    .setInputData(inputData)
                    .setInitialDelay(
                        delay,
                        TimeUnit.MILLISECONDS
                    )
                    .addTag(automation.id)
                    .build()
            }
        }

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                automation.id,
                ExistingWorkPolicy.REPLACE,
                request
            )
    }

    private fun cancelAutomation(id: String) {
        WorkManager.getInstance(context)
            .cancelAllWorkByTag(id)
    }

    private suspend fun executeAutomation(
        automation: Automation
    ) {
        val result = try {
            "success"
        } catch (e: Exception) {
            "error: ${e.message}"
        }

        val updated = automation.copy(
            lastRun = System.currentTimeMillis(),
            lastResult = result,
            runCount = automation.runCount + 1
        )

        dao.update(updated.toEntity())

        _automationsFlow.value =
            dao.getAll().map { it.toAutomation() }
    }

    private fun calculateDelay(
        targetHour: Int,
        targetMinute: Int
    ): Long {
        val cal = Calendar.getInstance()
        val now = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, targetHour)
        cal.set(Calendar.MINUTE, targetMinute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        var delay = cal.timeInMillis - now

        if (delay <= 0) {
            delay += 24 * 60 * 60 * 1000L
        }

        return delay
    }

    private fun calculateWeeklyDelay(
        dayOfWeek: Int,
        targetHour: Int,
        targetMinute: Int
    ): Long {
        val cal = Calendar.getInstance()
        val now = cal.timeInMillis

        cal.set(Calendar.DAY_OF_WEEK, dayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, targetHour)
        cal.set(Calendar.MINUTE, targetMinute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        var delay = cal.timeInMillis - now

        if (delay <= 0) {
            delay += 7 * 24 * 60 * 60 * 1000L
        }

        return delay
    }

    fun parseSchedule(input: String): AutomationSchedule? {
        val lower = input.lowercase()

        val dailyMatch = Regex(
            """every day at (\d{1,2})(?::(\d{2}))?\s*(am|pm)?""",
            RegexOption.IGNORE_CASE
        ).find(lower)

        if (dailyMatch != null) {
            var hour = dailyMatch.groupValues[1].toInt()
            val minute =
                dailyMatch.groupValues[2].toIntOrNull() ?: 0

            val isPM =
                dailyMatch.groupValues[3].lowercase() == "pm"

            if (isPM && hour != 12) hour += 12
            if (!isPM && hour == 12) hour = 0

            if (hour !in 0..23 || minute !in 0..59) {
                return null
            }

            return AutomationSchedule.Daily(hour, minute)
        }

        val intervalMatch = Regex(
            """every (\d+)\s*(minute|hour|day)s?""",
            RegexOption.IGNORE_CASE
        ).find(lower)

        if (intervalMatch != null) {
            val value =
                intervalMatch.groupValues[1].toLongOrNull()
                    ?: return null

            if (value <= 0) return null

            val unit = intervalMatch.groupValues[2]

            val ms = when (unit) {
                "minute" -> value * 60 * 1000L
                "hour" -> value * 60 * 60 * 1000L
                "day" -> value * 24 * 60 * 60 * 1000L
                else -> return null
            }

            return AutomationSchedule.Interval(ms)
        }

        return null
    }

    data class Automation(
        val id: String,
        val name: String,
        val command: String,
        val schedule: AutomationSchedule,
        val enabled: Boolean = true,
        val lastRun: Long? = null,
        val lastResult: String? = null,
        val runCount: Int = 0
    )

    sealed class AutomationSchedule {
        data class Daily(
            val hour: Int,
            val minute: Int
        ) : AutomationSchedule()

        data class Weekly(
            val dayOfWeek: Int,
            val hour: Int,
            val minute: Int
        ) : AutomationSchedule()

        data class Interval(
            val intervalMs: Long
        ) : AutomationSchedule()

        data class Once(
            val atMs: Long
        ) : AutomationSchedule()
    }
}
