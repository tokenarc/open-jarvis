# Graphify Memory System - Open Jarvis

## Overview

Graphify is Open Jarvis's persistent memory system built on SQLite via Room DB. It's designed as a graph database that tracks user behavior and learns patterns over time.

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                   GraphifyDB                        │
│              (Room Database)                       │
└─────────────────────────────────────────────────────┘
         ▲            ▲            ▲            ▲
         │            │            │            │
    ┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐
    │ AppNode│  │TaskNode│  │Contact │  │Pattern │
    │        │  │        │  │  Node  │  │  Node  │
    └────────┘  └────────┘  └────────┘  └────────┘
                                            ▲
                                            │
                                    ┌──────────┐
                                    │  Edges   │
                                    └──────────┘
```

## Node Types

### AppNode

Represents an installed application on the device.

```kotlin
@Entity(tableName = "app_nodes")
data class AppNode(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,      // "com.whatsapp"
    val label: String,          // "WhatsApp"
    val lastUsed: Long,        // timestamp
    val useCount: Int = 1     // times opened
)
```

**Index:** `packageName`, `lastUsed`

### TaskNode

Represents a user command or task execution.

```kotlin
@Entity(tableName = "task_nodes")
data class TaskNode(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val command: String,        // "Open WhatsApp"
    val result: String,       // "Success" or error
    val timestamp: Long,    // execution time
    val providerUsed: String  // "Groq" / null for M1
)
```

**Index:** `timestamp`, `command`

### ContactNode

Represents a contact from the device's contacts database.

```kotlin
@Entity(tableName = "contact_nodes")
data class ContactNode(
    @PrimaryKey
    val contactId: String,
    val displayName: String,
    val phoneNumber: String?,
    val email: String?,
    val lastMentioned: Long
)
```

### PatternNode

Represents learned patterns about user behavior.

```kotlin
@Entity(tableName = "pattern_nodes")
data class PatternNode(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val patternType: String,    // "APP_SEQUENCE"
    val patternData: String, // JSON: ["WhatsApp", "Chrome"]
    val confidence: Float,  // 0.0 - 1.0
    val lastUpdated: Long
)
```

## Edge Types

Edges connect nodes and represent relationships. They're stored as separate entities:

```kotlin
@Entity(tableName = "edges")
data class Edge(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sourceNodeId: Long,
    val targetNodeId: Long,
    val edgeType: String,     // "UsedBy", "OpenedAfter", etc.
    val weight: Float = 1.0f,
    val lastUpdated: Long
)
```

| Edge Type | Meaning | Example |
|----------|--------|---------|
| `UsedBy` | App Node used by Task | "WhatsApp" ← "Message John" |
| `OpenedAfter` | App opened after app | "Chrome" ← "Search" |
| `ResultedIn` | Task led to task | "Open WhatsApp" → "Send message" |
| `MentionedIn` | Contact mentioned in task | "John" ← "Message John" |
| `LearnedFrom` | Pattern derived from | (Pattern) ← [App history] |

## Query Patterns

### Get Recent Apps

```kotlin
@Query("SELECT * FROM app_nodes ORDER BY lastUsed DESC LIMIT :limit")
suspend fun getRecentApps(limit: Int): List<AppNode>
```

### Get Most Used Apps

```kotlin
@Query("SELECT * FROM app_nodes ORDER BY useCount DESC LIMIT :limit")
suspend fun getMostUsedApps(limit: Int): List<AppNode>
```

### Find Similar Tasks

```kotlin
@Query("SELECT * FROM task_nodes WHERE command LIKE '%' || :query || '%' ORDER BY timestamp DESC LIMIT :limit")
suspend fun findSimilarTasks(query: String, limit: Int): List<TaskNode>
```

### Get App Sequence Pattern

```kotlin
@Query("""
    SELECT a.* FROM app_nodes a
    INNER JOIN edges e ON a.id = e.targetNodeId
    WHERE e.sourceNodeId = :appId AND e.edgeType = 'OpenedAfter'
    ORDER BY e.weight DESC
""")
suspend fun getAppsOpenedAfter(appId: Long): List<AppNode>
```

## Memory Context Injection

When preparing a prompt for the LLM, inject relevant memory:

```kotlin
fun buildMemoryContext(recentApps: List<AppNode>, relevantTasks: List<TaskNode>): String {
    return buildString {
        appendLine("Recent Apps:")
        recentApps.take(5).forEach { app ->
            appendLine("- ${app.label} (used ${app.useCount}x)")
        }
        appendLine()
        appendLine("Relevant Past Tasks:")
        relevantTasks.take(3).forEach { task ->
            appendLine("- ${task.command}: ${task.result}")
        }
    }
}
```

**Example Memory Context:**
```
Recent Apps:
- WhatsApp (used 15x)
- Chrome (used 12x)
- Camera (used 8x)

Relevant Past Tasks:
- Open WhatsApp: Success
- Send message to John: Success
```

## Pattern Learning

### On Task Completion

```kotlin
suspend fun onTaskComplete(command: String, success: Boolean) {
    // Log task node
    taskNodeDao.insert(TaskNode(
        command = command,
        result = if (success) "Success" else "Failed",
        timestamp = System.currentTimeMillis()
    ))
    
    // If successful, update pattern
    if (success) {
        learnPattern(command)
    }
}
```

### Simple Pattern: App Sequence

Track when apps are opened in sequence:

```kotlin
suspend fun learnPattern(newApp: AppNode) {
    val previousRecent = appNodeDao.getRecentApps(2)
    if (previousRecent.size >= 2) {
        val previousApp = previousRecent[1]
        
        // Check if edge exists
        val existingEdge = edgeDao.findEdge(
            previousApp.id, 
            newApp.id, 
            "OpenedAfter"
        )
        
        if (existingEdge != null) {
            // Increment weight
            edgeDao.incrementWeight(existingEdge.id)
        } else {
            // Create new edge
            edgeDao.insert(Edge(
                sourceNodeId = previousApp.id,
                targetNodeId = newApp.id,
                edgeType = "OpenedAfter",
                weight = 1.0f
            ))
        }
    }
}
```

## Performance

- **Database size:** ~5MB for 1000 tasks, 50 apps
- **Query time:** <50ms for recent apps, <100ms for patterns
- **Write time:** <10ms per insertion

### Indexing Strategy

```kotlin
@Entity(tableName = "app_nodes", indices = [
    Index(value = ["packageName"], unique = true),
    Index(value = ["lastUsed"]),
    Index(value = ["useCount"])
])
```

### Room Configuration

```kotlin
Room.databaseBuilder(
    context,
    GraphifyDB::class.java,
    "graphify.db"
)
    .fallbackToDestructiveMigration()
    .build()
```

## Security

- All stored locally on device
- No cloud sync in M1-M8
- API keys stored in EncryptedSharedPreferences
- App nodes do not contain sensitive data
- Contact nodes opt-in only (M4+)

## Future Extensions

After M8, Graphify could add:

- **Graph Visualization:** Show user their usage as a graph
- **Sync:** Optional cloud backup of patterns
- **Sharing:** Share patterns between devices
- **NLP:** Natural language queries ("What apps do I use in the morning?")

---

## File Structure

```
graphify/
├── GraphifyDB.kt          # Room database
├── GraphifyRepository.kt   # Data access layer
└── nodes/
    ├── AppNode.kt
    ├── AppNodeDao.kt
    ├── TaskNode.kt
    ├── TaskNodeDao.kt
    ├── ContactNode.kt
    ├── PatternNode.kt
    └── Edge.kt
```