package com.openjarvis.intelligence

data class ExecutionPlan(
    val steps: List<ExecutionStep>,
    val reasoning: String,
    val scoredApps: List<TaskRouter.ScoredApp> = emptyList()
)

data class ExecutionStep(
    val appPackage: String,
    val appLabel: String,
    val stepType: StepType,
    val inputKey: String? = null,
    val outputKey: String? = null,
    val promptTemplate: String? = null
)

enum class StepType {
    OPEN_APP,
    AI_PROMPT,
    EXTRACT,
    WRITE,
    READ
}