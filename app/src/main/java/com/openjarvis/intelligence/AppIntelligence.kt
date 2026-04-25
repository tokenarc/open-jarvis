package com.openjarvis.intelligence

data class AppIntelligence(
    val packageName: String,
    val appName: String,
    val category: AppCategory,
    val capabilities: List<Capability>,
    val trustScore: Float = 0.5f,
    val lastAnalyzed: Long = System.currentTimeMillis(),
    val isAIApp: Boolean = false,
    val aiMeta: AIAppMeta? = null
)

enum class AppCategory {
    PRODUCTIVITY,
    COMMUNICATION,
    SOCIAL,
    ENTERTAINMENT,
    UTILITIES,
    SYSTEM,
    FILE_MANAGEMENT,
    BROWSER,
    IMAGE_GENERATION,
    MUSIC,
    VIDEO,
    NEWS,
    SHOPPING,
    HEALTH,
    FINANCE,
    EDUCATION,
    GAMES,
    WEATHER,
    CAMERA,
    EMAIL,
    CALENDAR,
    NOTES,
    UNKNOWN
}

enum class Capability {
    SEARCH,
    AI_REASONING,
    MESSAGING,
    FILE_READ,
    FILE_WRITE,
    NOTE_TAKING,
    NAVIGATION,
    PAYMENT,
    MEDIA_PLAY,
    WEB_BROWSE,
    IMAGE_CAPTURE,
    EMAIL,
    CALENDAR,
    TRANSLATE,
    VOICE_INPUT,
    VISION_OCR
}