# MCP Server — Open Jarvis

## Model Context Protocol (MCP)

MCP lets Open Jarvis connect to external services like Home Assistant, Notion, GitHub, and more. It provides a standardized way to call external APIs.

---

## Overview

MCP clients connect to servers that expose tools. Jarvis can call these tools via natural language:

```
"Turn on the living room lights" → MCP: home_assistant.lights.on
"Add task to Notion" → MCP: notion.tasks.create
```

---

## Supported Servers

| Server | URL | Tools |
|--------|-----|-------|
| Home Assistant | http://homeassistant.local:8123/api/mcp | lights, climate, scripts |
| Notion | https://api.notion.com/v1 | pages, databases, tasks |
| GitHub | https://api.github.com | issues, prs, repos |

---

## Configuration

### Home Assistant

1. Install MCP addon in Home Assistant
2. Get Long-Lived Access Token
3. Enter in Settings → MCP Servers:

```
Name: Home Assistant
URL: http://homeassistant.local:8123/api/mcp
Token: your-long-lived-token
```

### Notion

1. Create integration at notion.so/my-integrations
2. Share database/page with integration
3. Enter credentials:

```
Name: Notion
URL: https://api.notion.com/v1
Token: secret_xxxxxxxxxxxxx
```

---

## MCPManager

```kotlin
val mcpManager = MCPManager(context)

// List available servers
mcpManager.serverTemplates.forEach {
    println("${it.name}: ${it.baseUrl}")
}

// Connect to server
mcpManager.connect("home_assistant") { result ->
    result.onSuccess { client ->
        // Connected
    }
}

// Call tool
mcpManager.callTool("home_assistant", "lights.on", mapOf(
    "entity_id" to "light.living_room"
))
```

---

## MCPClient

```kotlin
class MCPClient(
    val serverName: String,
    val baseUrl: String,
    val apiKey: String
) {
    suspend fun listTools(): List<MCPTool>
    suspend fun callTool(name: String, args: Map<String, Any>): Result<Any>
    fun disconnect()
}
```

---

## Security

- API keys stored in EncryptedSharedPreferences
- Each server isolated in own client instance
- Rate limiting per server
- Request timeout: 30 seconds

---

## Error Handling

| Error | Meaning | Fix |
|-------|--------|-----|
| Connection refused | Server down | Check server status |
| 401 Unauthorized | Invalid token | Regenerate token |
| 429 Rate limited | Too many requests | Wait, retry |
| Timeout | Server slow | Increase timeout |

---

## Adding Custom MCP Server

1. Implement MCPServerTemplate
2. Define tool schema
3. Add to serverTemplates list
4. Test connection

---

## Tips

- Use descriptive tool names for natural language matching
- Keep tool arguments minimal
- Test each tool manually before adding
- Monitor usage in logs