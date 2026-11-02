
package com.openjarvis.mcp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class MCPClient(
    val server: MCPServer
) {
    private val client = okhttp3.OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private var isConnected = false
    private var availableTools = emptyList<MCPTool>()

    private val jsonMediaType = "application/json".toMediaType()

    suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            val requestBody = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("id", 1)
                put("method", "initialize")
                put("params", JSONObject().apply {
                    put("protocolVersion", "2024-11-05")
                    put("capabilities", JSONObject())
                    put("clientInfo", JSONObject().apply {
                        put("name", "open-jarvis")
                        put("version", "1.0.0")
                    })
                })
            }.toString()

            val request = Request.Builder()
                .url(server.url)
                .post(requestBody.toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext false
                }

                val body = response.body?.string()
                    ?: return@withContext false

                val json = JSONObject(body)

                if (json.has("error")) {
                    return@withContext false
                }

                isConnected = true
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun listTools(): List<MCPTool> = withContext(Dispatchers.IO) {
        if (!isConnected) {
            disconnect()
            return@withContext emptyList()
        }

        try {
            val requestBody = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("id", 2)
                put("method", "tools/list")
            }.toString()

            val request = Request.Builder()
                .url(server.url)
                .post(requestBody.toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext emptyList()
                }

                val body = response.body?.string()
                    ?: return@withContext emptyList()

                val json = JSONObject(body)
                val result = json.optJSONObject("result")
                val toolsArray = result?.optJSONArray("tools")
                    ?: JSONArray()

                availableTools = (0 until toolsArray.length()).map { i ->
                    val tool = toolsArray.getJSONObject(i)
                    MCPTool(
                        name = tool.getString("name"),
                        description = tool.optString("description", ""),
                        inputSchema = tool.optJSONObject("inputSchema")
                    )
                }

                availableTools
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun callTool(
        toolName: String,
        arguments: JSONObject
    ): String = withContext(Dispatchers.IO) {
        if (!isConnected) {
            disconnect()
            return@withContext "Error: Not connected"
        }

        try {
            val requestBody = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("id", 3)
                put("method", "tools/call")
                put("params", JSONObject().apply {
                    put("name", toolName)
                    put("arguments", arguments)
                })
            }.toString()

            val request = Request.Builder()
                .url(server.url)
                .post(requestBody.toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext "Error: HTTP ${response.code}"
                }

                val body = response.body?.string()
                    ?: return@withContext "Error: Empty response"

                val json = JSONObject(body)

                if (json.has("error")) {
                    return@withContext "Error: ${
                        json.getJSONObject("error").optString("message")
                    }"
                }

                val result = json.optJSONObject("result")
                val content = result?.optJSONArray("content")

                content?.let { items ->
                    (0 until items.length()).joinToString("\n") { index ->
                        items.getJSONObject(index).optString("text", "")
                    }
                } ?: "Tool executed"
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    fun disconnect() {
        isConnected = false
        availableTools = emptyList()
    }

    fun isConnected(): Boolean = isConnected

    fun getToolCount(): Int = availableTools.size

    companion object {
        const val HTTP = "http"
        const val SSE = "sse"
    }
}

data class MCPServer(
    val id: String,
    val name: String,
    val url: String,
    val apiKey: String? = null,
    val enabled: Boolean = true
)

data class MCPTool(
    val name: String,
    val description: String,
    val inputSchema: JSONObject? = null
)
