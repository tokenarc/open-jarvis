package com.openjarvis.bridge

import android.content.Context
import android.os.Build
import android.os.Process
import com.openjarvis.agent.AgentCore
import com.openjarvis.agent.AgentState
import com.openjarvis.graphify.GraphifyRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket

class SocketServer(
    private val context: Context,
    private val agentCore: AgentCore,
    private val graphifyRepo: GraphifyRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private var runningPort = 0
    
    private var lastRequestTime = 0L
    private var requestCount = 0
    
    private val socketFile: java.io.File
        get() = java.io.File(context.filesDir, SOCKET_NAME)
    
    private val allowedUids = setOf(
        context.applicationInfo.uid,
        2000,
        "com.termux".hashCode()
    )

    private fun isAuthorized(): Boolean {
        return try {
            val callingUid = Process.myUid()
            callingUid in allowedUids
        } catch (e: Exception) {
            false
        }
    }
    
    companion object {
        private const val SOCKET_NAME = "jarvis.port"
        private const val MAX_COMMAND_LENGTH = 2000
        private const val MAX_REQUESTS_PER_SECOND = 10
    }
    
    fun start() {
        if (isRunning) return
        
        isRunning = true
        scope.launch {
            acceptConnections()
        }
    }

    fun stop() {
        isRunning = false
        serverSocket?.close()
        serverSocket = null
        
        val sockFile = socketFile
        if (sockFile.exists()) {
            sockFile.delete()
        }
        
        scope.coroutineContext[Job]?.cancelChildren()
    }
    
    fun getPort(): Int = runningPort

    private suspend fun acceptConnections() = withContext(Dispatchers.IO) {
        try {
            serverSocket = ServerSocket(0)
            runningPort = serverSocket?.localPort ?: 0
            
            val sockFile = socketFile
            sockFile.writeText(runningPort.toString())
            
            while (isRunning) {
                try {
                    val client = serverSocket?.accept() ?: break
                    scope.launch { handleClient(client) }
                } catch (e: Exception) {
                    if (isRunning) {
                        delay(100)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun handleClient(clientSocket: Socket) = withContext(Dispatchers.IO) {
        try {
            val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
            val writer = PrintWriter(clientSocket.getOutputStream(), true)
            
            val line = reader.readLine() ?: run {
                writer.println("""{"error":"empty request"}""")
                return@withContext
            }

            if (!isAuthorized()) {
                writer.println(createErrorResponse("", "unauthorized"))
                return@withContext
            }

            val request = parseRequest(line)
            if (request == null) {
                writer.println(createErrorResponse("", "invalid JSON"))
                return@withContext
            }
            
            val (requestId, cmd) = request
            
            when (cmd.lowercase()) {
                "status" -> handleStatus(writer, requestId)
                "history" -> handleHistory(writer, requestId)
                "providers" -> handleProviders(writer, requestId)
                "memory" -> handleMemory(writer, requestId)
                else -> handleCommand(writer, requestId, cmd)
            }
            
            writer.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                clientSocket.close()
            } catch (e: Exception) { }
        }
    }

    private fun handleStatus(writer: PrintWriter, requestId: String) {
        val accessibilityEnabled = com.openjarvis.accessibility.JarvisAccessibilityService.instance != null
        val provider = try {
            agentCore.getCurrentProviderName()
        } catch (e: Exception) { "unknown" }
        
        val statusJson = buildString {
            append("{")
            append("\"requestId\": \"$requestId\",")
            append("\"status\": \"done\",")
            append("\"result\": {")
            append("\"service\": \"running\",")
            append("\"accessibility\": $accessibilityEnabled,")
            append("\"provider\": \"$provider\",")
            append("\"port\": $runningPort")
            append("}}")
        }
        
        writer.println(statusJson)
    }

    private suspend fun handleHistory(writer: PrintWriter, requestId: String) {
        val tasks = graphifyRepo.getRecentTasks(10)
        val tasksJson = tasks.joinToString(",", "[", "]") { task ->
            """{"id":${task.id},"command":"${escapeJson(task.command)}","result":"${escapeJson(task.result)}","timestamp":${task.timestamp}}"""
        }
        
        writer.println("""{"requestId":"$requestId","status":"done","result":$tasksJson}""")
    }

    private suspend fun handleProviders(writer: PrintWriter, requestId: String) {
        val providers = graphifyRepo.getProviderStats()
        val providersJson = providers.joinToString(",", "[", "]") { p ->
            """{"name":"${p.name}","useCount":${p.useCount},"successRate":${p.successRate}}"""
        }
        
        writer.println("""{"requestId":"$requestId","status":"done","result":$providersJson}""")
    }

    private suspend fun handleMemory(writer: PrintWriter, requestId: String) {
        val context = graphifyRepo.buildMemoryContext("")
        val contextJson = """{"requestId":"$requestId","status":"done","result":"${escapeJson(context)}"}"""
        
        writer.println(contextJson)
    }

    private suspend fun handleCommand(writer: PrintWriter, requestId: String, cmd: String) {
        val job = scope.launch {
            agentCore.getStateFlow().collectLatest { state ->
                when (state) {
                    is AgentState.Running -> {
                        writer.println("""{"requestId":"$requestId","status":"progress","result":"${state.step}"}""")
                        writer.flush()
                    }
                    is AgentState.Done -> {
                        writer.println("""{"requestId":"$requestId","status":"done","result":"${escapeJson(state.result)}"}""")
                        writer.flush()
                        cancel()
                    }
                    is AgentState.Error -> {
                        writer.println("""{"requestId":"$requestId","status":"error","result":"${escapeJson(state.message)}""")
                        writer.flush()
                        cancel()
                    }
                    is AgentState.Idle -> { }
                }
            }
        }
        
        agentCore.executeTask(cmd)
        
        job.join()
    }

    private fun parseRequest(line: String): Pair<String, String>? {
        return try {
            val json = org.json.JSONObject(line)
            val cmd = json.optString("cmd", "")
            val requestId = json.optString("requestId", "")
            
            if (cmd.isBlank()) null
            else Pair(requestId, cmd)
        } catch (e: Exception) {
            null
        }
    }

    internal fun createErrorResponse(requestId: String, error: String): String {
        return """{"requestId":"$requestId","status":"error","result":"$error"}"""
    }

    internal fun escapeJson(s: String): String {
        return s
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    companion object {
        const val SOCKET_NAME = "jarvis.port"
    }
}