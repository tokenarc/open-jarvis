package com.openjarvis.graphify

import android.content.Context
import com.openjarvis.graphify.nodes.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.Calendar

class AnalysisEngine(context: Context) {

    private val repo = GraphifyRepository(context)

    suspend fun analyzeLastTask() = withContext(Dispatchers.IO) {
        val recentTasks = repo.getRecentTasks(10)
        if (recentTasks.size < 3) return@withContext

        detectSequencePatterns(recentTasks)
        detectTimeCorrelation(recentTasks)
        extractContacts(recentTasks)
    }

    private suspend fun detectSequencePatterns(tasks: List<TaskNode>) {
        if (tasks.size < 3) return

        val sequences = mutableListOf<String>()
        for (i in 0 until tasks.size - 2) {
            sequences.add(listOf(tasks[i].command, tasks[i + 1].command, tasks[i + 2].command)
                .joinToString(" → "))
        }

        val hashCounts = mutableMapOf<String, Int>()
        for (seq in sequences) {
            val hash = hashSequence(seq)
            hashCounts[hash] = hashCounts.getOrDefault(hash, 0) + 1
        }

        for ((sequence, count) in hashCounts) {
            if (count >= 2) {
                val existing = repo.getPatternByHash(hashSequence(sequence))
                val now = System.currentTimeMillis()

                if (existing != null) {
                    val newConfidence = (existing.confidence + 0.15f).coerceAtMost(1f)
                    repo.updatePatternConfidence(existing.id, newConfidence, now)
                } else {
                    val hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
                    val timeMask = 1 shl hourOfDay
                    val dayMask = 1 shl dayOfWeek

                    repo.insertPattern(
                        PatternNode(
                            sequenceHash = hashSequence(sequence),
                            sequenceString = sequence,
                            occurrenceCount = count,
                            lastSeen = now,
                            confidence = 0.5f,
                            timeOfDayMask = timeMask,
                            dayOfWeekMask = dayMask
                        )
                    )
                }
            }
        }
    }

    private suspend fun detectTimeCorrelation(tasks: List<TaskNode>) {
        if (tasks.size < 3) return

        val hourCounts = mutableMapOf<Int, Int>()
        for (task in tasks) {
            val cal = Calendar.getInstance().apply { timeInMillis = task.timestamp }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            hourCounts[hour] = hourCounts.getOrDefault(hour, 0) + 1
        }

        val peakHour = hourCounts.maxByOrNull { it.value }?.key ?: return
        val patterns = repo.getActivePatterns(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000)

        for (pattern in patterns) {
            if (pattern.confidence > 0.6f && (pattern.timeOfDayMask shr peakHour and 1) == 0) {
                val newMask = pattern.timeOfDayMask or (1 shl peakHour)
                repo.updatePatternTimeMask(pattern.id, newMask)
            }
        }
    }

    private suspend fun extractContacts(tasks: List<TaskNode>) {
        val contactPatterns = listOf(
            Regex("""call\s+([A-Za-z\s]+)"""),
            Regex("""text\s+([A-Za-z\s]+)"""),
            Regex("""message\s+([A-Za-z\s]+)"""),
            Regex("""whatsapp\s+([A-Za-z\s]+)"""),
            Regex("""send\s+to\s+([A-Za-z\s]+)""")
        )

        for (task in tasks) {
            for (pattern in contactPatterns) {
                val match = pattern.find(task.command.lowercase()) ?: continue
                val name = match.groupValues.getOrNull(1)?.trim() ?: continue
                if (name.isBlank()) continue

                val dist = levenshteinDistance(name, "john")
                val existing = repo.findContactByName(name)
                
                if (existing != null) {
                    repo.incrementContact(existing.id)
                } else {
                    repo.insertContact(
                        ContactNode(
                            name = name.replaceFirstChar { it.uppercase() },
                            lastContacted = task.timestamp,
                            contactMethod = if (task.command.contains("whatsapp")) "whatsapp" else "call"
                        )
                    )
                }

                val taskNode = tasks.find { it.command == task.command }
                if (taskNode != null) {
                    val contactNode = repo.findContactByName(name)
                    if (contactNode != null) {
                        repo.insertEdge(
                            EdgeEntity(
                                fromId = taskNode.id,
                                fromType = "task",
                                toId = contactNode.id,
                                toType = "contact",
                                edgeType = EdgeTypes.TASK_TO_CONTACT
                            )
                        )
                    }
                }

                break
            }
        }
    }

    suspend fun decayAllPatterns() = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000
        val patterns = repo.getAllPatterns()

        for (pattern in patterns) {
            if (pattern.lastSeen < cutoff) {
                val newConfidence = (pattern.confidence * 0.8f).coerceAtLeast(0.1f)
                repo.updatePatternConfidence(pattern.id, newConfidence)
            }
        }
    }

    private fun hashSequence(sequence: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(sequence.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }.take(16)
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length
        val dp = Array(len1 + 1) { IntArray(len2 + 1) }

        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j

        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[len1][len2]
    }
}